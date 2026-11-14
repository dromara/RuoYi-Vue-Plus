declare module '*.vue' {
  import { DefineComponent } from 'vue';
  const Component: DefineComponent<{}, {}, any>;
  export default Component;
}

declare module 'virtual:svg-icons-register' {}
declare module 'virtual:*' {}

// 环境变量
interface ImportMetaEnv {
  VITE_APP_TITLE: string;
  VITE_APP_PORT: number;
  VITE_APP_BASE_API: string;
  VITE_APP_BASE_URL: string;
  VITE_APP_CONTEXT_PATH: string;
  VITE_APP_MONITOR_ADMIN: string;
  VITE_APP_SNAILJOB_ADMIN: string;
  VITE_APP_SNAILAI_ADMIN: string;
  VITE_APP_ENV: string;
  VITE_APP_ENCRYPT: string;
  VITE_APP_RSA_PUBLIC_KEY: string;
  VITE_APP_RSA_PRIVATE_KEY: string;
  VITE_APP_CLIENT_ID: string;
  VITE_APP_MESSAGE_ENABLED: string;
  VITE_APP_MESSAGE_TRANSPORT: string;
  VITE_APP_MESSAGE_PATH: string;
}
interface ImportMeta {
  readonly env: ImportMetaEnv;
  // readonly glob: any;
}
