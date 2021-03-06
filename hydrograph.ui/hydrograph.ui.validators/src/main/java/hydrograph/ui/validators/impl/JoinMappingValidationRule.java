/*******************************************************************************
 * Copyright 2017 Capital One Services, LLC and Bitwise, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

 
package hydrograph.ui.validators.impl;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import hydrograph.ui.common.util.ParameterUtil;
import hydrograph.ui.datastructure.property.FilterProperties;
import hydrograph.ui.datastructure.property.FixedWidthGridRow;
import hydrograph.ui.datastructure.property.JoinMappingGrid;
import hydrograph.ui.datastructure.property.LookupMapProperty;


public class JoinMappingValidationRule implements IValidator{

	private static final String INPUT_PORT = "in";
	private String errorMessage;
	
	@Override
	public boolean validateMap(Object object, String propertyName,Map<String,List<FixedWidthGridRow>> inputSchemaMap) {
		Map<String, Object> propertyMap = (Map<String, Object>) object;
		if(propertyMap != null && !propertyMap.isEmpty()){ 
			return validate(propertyMap.get(propertyName), propertyName,inputSchemaMap,false);
		}
		return false;
	}


	@Override
	public boolean validate(Object object, String propertyName,Map<String,List<FixedWidthGridRow>> inputSchemaMap
			,boolean isJobImported){
		JoinMappingGrid joinMappingGrid = (JoinMappingGrid)object;
		if(joinMappingGrid == null){
			errorMessage = propertyName + " is mandatory";
			return false;
		}
		
		
		if(isJobImported) {
			List<List<FilterProperties>> listofFiledNameList = new ArrayList<>();
			Set<String> keySet = inputSchemaMap.keySet();
			for (String inSocketId : keySet) {
				List<FilterProperties> filedNameList = new ArrayList<>();
				List<FixedWidthGridRow> fixedWidthGridRows = inputSchemaMap.get(inSocketId);
				for (FixedWidthGridRow fixedWidthGridRow : fixedWidthGridRows) {
					FilterProperties filedName = new FilterProperties();
					filedName.setPropertyname(fixedWidthGridRow.getFieldName());
					filedNameList.add(filedName);
				}
				listofFiledNameList.add(getIndex(inSocketId), filedNameList);
			}
			joinMappingGrid.setLookupInputProperties(listofFiledNameList);
			isJobImported = false;
		}
		
		if(joinMappingGrid.isSelected() && joinMappingGrid.getButtonText()!=null){
			return true;
		}
		
		List<List<FilterProperties>> lookupInputProperties = joinMappingGrid.getLookupInputProperties();
		List<LookupMapProperty> lookupMapProperties = joinMappingGrid.getLookupMapProperties();
		if(lookupInputProperties == null || 
				lookupInputProperties.isEmpty() || lookupInputProperties.size() < 2){
			errorMessage = "Invalid input for join component"; 
			return false;
		}
		if(lookupMapProperties == null || lookupMapProperties.isEmpty()){
			errorMessage = "Invalid output from join component"; 
			return false;
		}
		
		for (List<FilterProperties> input : lookupInputProperties) {
			if(input == null || input.size() == 0){
				errorMessage = "Input mapping is mandatory";
				return false;
			}
			for(FilterProperties properties  : input){
				if (StringUtils.isBlank(properties.getPropertyname())) {
					errorMessage = "Input mapping is mandatory";
					return false;
				}
			}
		}
		
		for (LookupMapProperty lookupMapProperty : lookupMapProperties) {
			if (StringUtils.isBlank(lookupMapProperty.getSource_Field()) || StringUtils.isBlank(lookupMapProperty.getOutput_Field())) {
				errorMessage = "Output names are mandatory";
				return false;
			}
		}
		
		if(hasInvalidInputFields(getAllInputFieldNames(lookupInputProperties), lookupMapProperties)){
			errorMessage = "Invalid input fields in join mapping";
			return false;
		}
		
		if(isOutputFieldInvalid(lookupMapProperties)){
			errorMessage = "Invalid output fields in join mapping";
			return false;
		}
		
		return true;
	}

	private int getIndex(String inSocketId) {
		int index = 0;
		if (inSocketId.startsWith("in")) {
			index = Integer.parseInt(inSocketId.substring(2, inSocketId.length()));
		}
		return index;
	}


	@Override
	public String getErrorMessage() {
		return errorMessage;
	}
	
	private List<String> getAllInputFieldNames(List<List<FilterProperties>> lookupInputProperties){
		List<String> inputFieldList = new LinkedList<>();
		
		for(int i=0; i < lookupInputProperties.size();i++){
			List<FilterProperties> inputPortFieldList=lookupInputProperties.get(i);
			for(FilterProperties inField: inputPortFieldList){
				inputFieldList.add(INPUT_PORT + i + "." + inField.getPropertyname());
			}
		}
		
		return inputFieldList;
	}
	
	private boolean hasInvalidInputFields(List<String> allInputFields, List<LookupMapProperty> mappingTableItemList){
		for(LookupMapProperty mapRow: mappingTableItemList){
			if (!allInputFields.contains(mapRow
					.getSource_Field()) && !ParameterUtil.isParameter(mapRow.getSource_Field())) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isOutputFieldInvalid(List<LookupMapProperty> mappingTableItemList){
		List<String> outputFieldList = new ArrayList<>();
		for(LookupMapProperty mapRow: mappingTableItemList){
			if(outputFieldList.contains(mapRow.getOutput_Field())){
				return true;
			}else{
				outputFieldList.add(mapRow.getOutput_Field());
			}
		}
		
		return false;
	}
}
