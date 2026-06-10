/// <reference types="vite/client" />

// 声明 .vue 文件的类型，让 TypeScript 能识别 Vue SFC
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}
