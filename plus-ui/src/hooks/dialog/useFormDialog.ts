import type { Ref } from 'vue';
import { useDialogState } from './useDialogState';

interface UseFormDialogOptions<T extends Record<string, any>> {
  form: Ref<T>;
  formRef?: Ref<ElFormInstance | undefined>;
  initialFormData: T;
  initialTitle?: string;
}

export function useFormDialog<T extends Record<string, any>>(options: UseFormDialogOptions<T>) {
  const { form, formRef, initialFormData, initialTitle } = options;
  const { dialog, openDialog, closeDialog, setTitle } = useDialogState(initialTitle);

  const resetForm = () => {
    form.value = { ...initialFormData };
    formRef?.value?.resetFields();
    formRef?.value?.clearValidate?.();
  };

  const openFormDialog = (title?: string) => {
    resetForm();
    openDialog(title);
  };

  const showDialog = (title?: string) => {
    openDialog(title);
  };

  const closeFormDialog = () => {
    closeDialog();
  };

  return {
    dialog,
    resetForm,
    openDialog: openFormDialog,
    showDialog,
    closeDialog: closeFormDialog,
    setTitle
  };
}
