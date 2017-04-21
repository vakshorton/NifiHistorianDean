/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hortonworks.historian.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasException;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.typesystem.TypesDef;
import org.apache.atlas.typesystem.json.TypesSerialization;
import org.apache.atlas.typesystem.types.AttributeDefinition;
import org.apache.atlas.typesystem.types.ClassType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.typesystem.types.EnumType;
import org.apache.atlas.typesystem.types.EnumTypeDefinition;
import org.apache.atlas.typesystem.types.HierarchicalTypeDefinition;
import org.apache.atlas.typesystem.types.Multiplicity;
import org.apache.atlas.typesystem.types.StructType;
import org.apache.atlas.typesystem.types.StructTypeDefinition;
import org.apache.atlas.typesystem.types.TraitType;
import org.apache.atlas.typesystem.types.utils.TypesUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility that generates a data model for Apache NiFi.
 */
public class HistorianModelGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(HistorianModelGenerator.class);
    private static final DataTypes.MapType STRING_MAP_TYPE = new DataTypes.MapType(DataTypes.STRING_TYPE, DataTypes.STRING_TYPE);

    public static final String SOURCE = "source";
    public static final String DESTINATION = "destination";
    public static final String PROPERTIES = "parameters";

    private Map<String, EnumTypeDefinition> enumTypeDefinitionMap = new HashMap<String, EnumTypeDefinition>();
	private Map<String, StructTypeDefinition> structTypeDefinitionMap = new HashMap<String, StructTypeDefinition>();
	private Map<String, HierarchicalTypeDefinition<ClassType>> classTypeDefinitions = new HashMap<String, HierarchicalTypeDefinition<ClassType>>();
    
    public HistorianModelGenerator() {
        classTypeDefinitions = new HashMap<>();
        enumTypeDefinitionMap = new HashMap<>();
        structTypeDefinitionMap = new HashMap<>();
    }

    public String createDataModel() throws AtlasException {
    	TypesDef typesDef;
		String historianDataModelJSON;
	
		createAssetClass();
		createTagClass();
		createTagAttributeClass();
		
		typesDef = TypesUtil.getTypesDef(
				getEnumTypeDefinitions(), 	//Enums 
				getStructTypeDefinitions(), //Struct 
				getTraitTypeDefinitions(), 	//Traits 
				ImmutableList.copyOf(classTypeDefinitions.values()));
		
		historianDataModelJSON = TypesSerialization.toJson(typesDef);
		LOG.info("Submitting Types Definition: " + historianDataModelJSON);
		LOG.info("Generating the Historian Data Model....");
		return historianDataModelJSON;
    }

    private void createAssetClass() throws AtlasException {
        final String typeName = HistorianDataTypes.HISTORIAN_ASSET.getName();
        
        final AttributeDefinition[] attributeDefinitions = new AttributeDefinition[] {
                new AttributeDefinition("parent_assets", DataTypes.arrayTypeName(HistorianDataTypes.HISTORIAN_ASSET.getName()), Multiplicity.OPTIONAL, true, null),
                new AttributeDefinition("child_assets", DataTypes.arrayTypeName(HistorianDataTypes.HISTORIAN_ASSET.getName()), Multiplicity.OPTIONAL, true, null),
                new AttributeDefinition("historian_tags", DataTypes.arrayTypeName(HistorianDataTypes.HISTORIAN_TAG.getName()), Multiplicity.OPTIONAL, true, null)
        };
        
        addClassTypeDefinition(typeName, ImmutableSet.of(AtlasClient.REFERENCEABLE_SUPER_TYPE), attributeDefinitions);
        LOG.info("Created definition for " + typeName);
    }

    private void createTagClass() throws  AtlasException {
        final String typeName = HistorianDataTypes.HISTORIAN_TAG.getName();

        final AttributeDefinition[] attributeDefinitions = new AttributeDefinition[] {
        		new AttributeDefinition("parent_assets", DataTypes.arrayTypeName(HistorianDataTypes.HISTORIAN_ASSET.getName()), Multiplicity.OPTIONAL, true, null),
                new AttributeDefinition("tags_attributes", DataTypes.arrayTypeName(HistorianDataTypes.HISTORIAN_TAG_ATTRIBUTE.getName()), Multiplicity.OPTIONAL, true, null)
        };

        addClassTypeDefinition(typeName, ImmutableSet.of(AtlasClient.REFERENCEABLE_SUPER_TYPE), attributeDefinitions);
        LOG.info("Created definition for " + typeName);
    }

    private void createTagAttributeClass() throws AtlasException {
        final String typeName = HistorianDataTypes.HISTORIAN_TAG_ATTRIBUTE.getName();

        final AttributeDefinition[] attributeDefinitions = new AttributeDefinition[] {
        		new AttributeDefinition("associated_tags", DataTypes.arrayTypeName(HistorianDataTypes.HISTORIAN_TAG.getName()), Multiplicity.OPTIONAL, true, null),
                new AttributeDefinition(PROPERTIES, STRING_MAP_TYPE.getName(), Multiplicity.OPTIONAL, false, null)
        };

        addClassTypeDefinition(typeName, ImmutableSet.of(AtlasClient.REFERENCEABLE_SUPER_TYPE), attributeDefinitions);
        LOG.info("Created definition for " + typeName);
    }

    private void addClassTypeDefinition(String typeName, ImmutableSet<String> superTypes, AttributeDefinition[] attributeDefinitions) {
        final HierarchicalTypeDefinition<ClassType> definition =
                new HierarchicalTypeDefinition<>(ClassType.class, typeName, null, superTypes, attributeDefinitions);

        classTypeDefinitions.put(typeName, definition);
    }

    public TypesDef getTypesDef() {
        return TypesUtil.getTypesDef(getEnumTypeDefinitions(), getStructTypeDefinitions(), getTraitTypeDefinitions(), getClassTypeDefinitions());
    }

    public String getDataModelAsJSON() {
        return TypesSerialization.toJson(getTypesDef());
    }

    public ImmutableList<EnumTypeDefinition> getEnumTypeDefinitions() {
        return ImmutableList.copyOf(enumTypeDefinitionMap.values());
    }

    public ImmutableList<StructTypeDefinition> getStructTypeDefinitions() {
        return ImmutableList.copyOf(structTypeDefinitionMap.values());
    }

    public ImmutableList<HierarchicalTypeDefinition<ClassType>> getClassTypeDefinitions() {
        return ImmutableList.copyOf(classTypeDefinitions.values());
    }

    public ImmutableList<HierarchicalTypeDefinition<TraitType>> getTraitTypeDefinitions() {
        return ImmutableList.of();
    }

    public String getModelAsJson() throws AtlasException {
        createDataModel();
        return getDataModelAsJSON();
    }

    public static void main(String[] args) throws Exception {
        HistorianModelGenerator HistorianDataModelGenerator = new HistorianModelGenerator();
        System.out.println("HistorianDataModelAsJSON = " + HistorianDataModelGenerator.getModelAsJson());

        TypesDef typesDef = HistorianDataModelGenerator.getTypesDef();
        for (EnumTypeDefinition enumType : typesDef.enumTypesAsJavaList()) {
            System.out.println(String.format("%s(%s) - values %s", enumType.name, EnumType.class.getSimpleName(),
                    Arrays.toString(enumType.enumValues)));
        }
        for (StructTypeDefinition structType : typesDef.structTypesAsJavaList()) {
            System.out.println(String.format("%s(%s) - attributes %s", structType.typeName, StructType.class.getSimpleName(),
                    Arrays.toString(structType.attributeDefinitions)));
        }
        for (HierarchicalTypeDefinition<ClassType> classType : typesDef.classTypesAsJavaList()) {
            System.out.println(String.format("%s(%s) - super types [%s] - attributes %s", classType.typeName, ClassType.class.getSimpleName(),
                    StringUtils.join(classType.superTypes, ","), Arrays.toString(classType.attributeDefinitions)));
        }
        for (HierarchicalTypeDefinition<TraitType> traitType : typesDef.traitTypesAsJavaList()) {
            System.out.println(String.format("%s(%s) - %s", traitType.typeName, TraitType.class.getSimpleName(),
                    Arrays.toString(traitType.attributeDefinitions)));
        }
    }
}
