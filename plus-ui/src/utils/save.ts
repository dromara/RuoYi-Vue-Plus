export function saveBlob(blob: Blob, fileName: string) {
  const downloadLink = document.createElement('a');
  const objectUrl = URL.createObjectURL(blob);

  downloadLink.href = objectUrl;
  downloadLink.download = fileName;
  downloadLink.rel = 'noopener';
  downloadLink.click();

  setTimeout(() => {
    URL.revokeObjectURL(objectUrl);
  }, 40_000);
}
