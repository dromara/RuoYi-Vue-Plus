import { ecb } from '@noble/ciphers/aes.js';

/**
 * 随机生成32位的字符串
 * @returns {string}
 */
const generateRandomString = (): string => {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return Array.from(array, b => b.toString(16).padStart(2, '0'))
    .join('')
    .slice(0, 32);
};

const encoder = new TextEncoder();
const decoder = new TextDecoder();

const bytesToBase64 = (bytes: Uint8Array): string => {
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary);
};

const base64ToBytes = (str: string): Uint8Array => {
  const binary = atob(str);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
};

/**
 * 随机生成aes 密钥
 * @returns {Uint8Array}
 */
export const generateAesKey = (): Uint8Array => {
  return encoder.encode(generateRandomString());
};

/**
 * 加密base64
 * @returns {string}
 */
export const encryptBase64 = (bytes: Uint8Array): string => {
  return bytesToBase64(bytes);
};

/**
 * 解密base64
 */
export const decryptBase64 = (str: string): Uint8Array => {
  return base64ToBytes(str);
};

/**
 * 使用密钥对数据进行加密 (AES/ECB/PKCS7Padding 与后端保持一致)
 * @param message
 * @param aesKey
 * @returns {string}
 */
export const encryptWithAes = (message: string, aesKey: Uint8Array): string => {
  const encrypted = ecb(aesKey).encrypt(encoder.encode(message));
  return bytesToBase64(encrypted);
};

/**
 * 使用密钥对数据进行解密
 * @param message
 * @param aesKey
 * @returns {string}
 */
export const decryptWithAes = (message: string, aesKey: Uint8Array): string => {
  const decrypted = ecb(aesKey).decrypt(base64ToBytes(message));
  return decoder.decode(decrypted);
};
