package com.liferay.portal.typeconverter;

import com.liferay.dynamic.data.mapping.kernel.DDMFormValues;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.kernel.DDMFormFieldValue;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.dynamic.data.mapping.kernel.DDMStructure;
import com.liferay.portal.kernel.json.JSONArray;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import jodd.typeconverter.TypeConverter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.service.component.annotations.Reference;

public class DDMFormValuesConverter implements TypeConverter<DDMFormValues> {

	@Override
	public DDMFormValues convert(Object object) {
		if (object == null) {
			return null;
		}

		if (object instanceof DDMFormValues) {
			return (DDMFormValues)object;
		}

		long recordSetId = ParamUtil.getLong(object, "recordSetId");

		DDLRecordSet recordSet = ddlRecordSetService.getRecordSet(recordSetId);

		DDMStructure ddmStructure = recordSet.getDDMStructure();

		DDMForm ddmForm = ddmStructure.getFullHierarchyDDMForm();

		DDMFormValues ddmFormValues = new DDMFormValues(ddmForm);

		JSONObject jsonObject = _jsonFactory.createJSONObject(
			ddmFormValuesDeserializerDeserializeRequest.getContent());

		_setDDMFormValuesAvailableLocales(
			jsonObject.getJSONArray("availableLanguageIds"), ddmFormValues);
		_setDDMFormValuesDefaultLocale(
			jsonObject.getString("defaultLanguageId"), ddmFormValues);
		setDDMFormFieldValues(
			jsonObject.getJSONArray("fieldValues"), ddmForm, ddmFormValues);

		setDDMFormLocalizedValuesDefaultLocale(ddmFormValues);

		return ddmFormValues;
	}

	protected Set<Locale> getAvailableLocales(JSONArray jsonArray) {
		Set<Locale> availableLocales = new HashSet<>();

		if (jsonArray == null) {
			return availableLocales;
		}

		for (int i = 0; i < jsonArray.length(); i++) {
			Locale availableLocale = LocaleUtil.fromLanguageId(
				jsonArray.getString(i));

			availableLocales.add(availableLocale);
		}

		return availableLocales;
	}

	protected List<DDMFormFieldValue> getDDMFormFieldValues(
		JSONArray jsonArray, Map<String, DDMFormField> ddmFormFieldsMap) {

		List<DDMFormFieldValue> ddmFormFieldValues = new ArrayList<>();

		for (int i = 0; i < jsonArray.length(); i++) {
			ddmFormFieldValues.add(
				_getDDMFormFieldValue(
					jsonArray.getJSONObject(i), ddmFormFieldsMap));
		}

		return ddmFormFieldValues;
	}

	private void _setDDMFormValuesAvailableLocales(
		JSONArray jsonArray, DDMFormValues ddmFormValues) {

		ddmFormValues.setAvailableLocales(getAvailableLocales(jsonArray));
	}

	private void _setDDMFormValuesDefaultLocale(
		String defaultLanguageId, DDMFormValues ddmFormValues) {

		Locale defaultLocale = LocaleUtil.fromLanguageId(defaultLanguageId);

		ddmFormValues.setDefaultLocale(defaultLocale);

		Set<Locale> availableLocales = ddmFormValues.getAvailableLocales();

		if ((availableLocales != null) &&
			!availableLocales.contains(defaultLocale)) {

			availableLocales.add(defaultLocale);
		}
	}

	protected void setDDMFormFieldValues(
		JSONArray jsonArray, DDMForm ddmForm, DDMFormValues ddmFormValues) {

		ddmFormValues.setDDMFormFieldValues(
			getDDMFormFieldValues(
				jsonArray, ddmForm.getDDMFormFieldsMap(true)));
	}

	private DDMFormFieldValue _getDDMFormFieldValue(
		JSONObject jsonObject, Map<String, DDMFormField> ddmFormFieldsMap) {

		DDMFormFieldValue ddmFormFieldValue = new DDMFormFieldValue();

		ddmFormFieldValue.setFieldReference(
			jsonObject.getString("fieldReference"));

		String instanceId = jsonObject.getString("instanceId");

		if (instanceId.matches("[a-zA-Z0-9]*")) {
			ddmFormFieldValue.setInstanceId(instanceId);
		}

		ddmFormFieldValue.setName(jsonObject.getString("name"));

		_setDDMFormFieldValueValue(
			jsonObject, ddmFormFieldsMap.get(jsonObject.getString("name")),
			ddmFormFieldValue);

		_setNestedDDMFormFieldValues(
			jsonObject.getJSONArray("nestedFieldValues"), ddmFormFieldsMap,
			ddmFormFieldValue);

		return ddmFormFieldValue;
	}

//	@Reference
//	private JSONFactory _jsonFactory;
}
