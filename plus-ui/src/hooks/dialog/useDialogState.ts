export function useDialogState(initialTitle = '') {
  const dialog = reactive<DialogOption>({
    visible: false,
    title: initialTitle
  });

  const openDialog = (title?: string) => {
    if (title !== undefined) {
      dialog.title = title;
    }
    dialog.visible = true;
  };

  const closeDialog = () => {
    dialog.visible = false;
  };

  const toggleDialog = () => {
    dialog.visible = !dialog.visible;
  };

  const setTitle = (title: string) => {
    dialog.title = title;
  };

  return {
    dialog,
    openDialog,
    closeDialog,
    toggleDialog,
    setTitle
  };
}
