import type * as ep from 'element-plus';
declare global {
  type ElTagType = 'primary' | 'success' | 'info' | 'warning' | 'danger';
  type ElFormInstance = ep.FormInstance;
  type ElTableInstance = ep.TableInstance;
  type ElUploadInstance = ep.UploadInstance;
  type ElScrollbarInstance = ep.ScrollbarInstance;
  type ElInputInstance = ep.InputInstance;
  type ElInputNumberInstance = ep.InputNumberInstance;
  type ElRadioInstance = ep.RadioInstance;
  type ElRadioGroupInstance = ep.RadioGroupInstance;
  type ElRadioButtonInstance = ep.RadioButtonInstance;
  type ElCheckboxInstance = ep.CheckboxInstance;
  type ElSwitchInstance = ep.SwitchInstance;
  type ElCascaderInstance = ep.CascaderInstance;
  type ElColorPickerInstance = ep.ColorPickerInstance;
  type ElRateInstance = ep.RateInstance;
  type ElSliderInstance = ep.SliderInstance;

  type ElTreeInstance = InstanceType<typeof ep.ElTree>;
  type ElTreeSelectInstance = InstanceType<typeof ep.ElTreeSelect>;
  type ElSelectInstance = InstanceType<typeof ep.ElSelect>;
  type ElCardInstance = InstanceType<typeof ep.ElCard>;
  type ElDialogInstance = InstanceType<typeof ep.ElDialog>;
  type ElCheckboxGroupInstance = InstanceType<typeof ep.ElCheckboxGroup>;
  type ElDatePickerInstance = InstanceType<typeof ep.ElDatePicker>;
  type ElTimePickerInstance = InstanceType<typeof ep.ElTimePicker>;
  type ElTimeSelectInstance = InstanceType<typeof ep.ElTimeSelect>;

  type TransferKey = ep.TransferKey;
  type CheckboxValueType = ep.CheckboxValueType;
  type ElFormRules = ep.FormRules;
  type DateModelType = ep.DateModelType;
  type UploadFile = ep.UploadFile;
}
