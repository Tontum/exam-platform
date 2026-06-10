/**
 * 项目服务 API — 对接 exam-project-service（端口 8087）
 * 包含项目管理、配置管理、加入/退出项目
 */
import { get, post, put, del } from './index'

// ==================== 类型定义 ====================

/** 后端返回的项目 VO */
export interface ProjectVO {
  id: string
  name: string
  description: string
  creatorId: string
  province: string
  city: string
  /** 0=未开始、1=进行中、2=已结束 */
  status: number
  createdAt: string
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 配置 VO */
export interface ConfigVO {
  id: string
  toolId: string
  toolCode: string
  toolName: string
  role: number
  isEnabled: number
  allowPublish: number
  allowDelete: number
  allowReview: number
  requirePassScore: number
  autoScore: number
  publishTimeStart: string | null
  publishTimeEnd: string | null
  scorePerSubmit: number
}

// ==================== 项目管理 API ====================

/** 分页查询项目列表（管理员用） */
export function listProjects(params: { page?: number; size?: number; status?: number; keyword?: string; province?: string; city?: string }) {
  return get<PageResult<ProjectVO>>('/project/list', params)
}

/** 查询当前用户参与的项目列表（校长/老师用） */
export function getMyProjects() {
  return get<ProjectVO[]>('/project/my')
}

/** 查询项目详情 */
export function getProject(id: string) {
  return get<ProjectVO>(`/project/${id}`)
}

/** 创建项目 */
export function createProject(data: { 
  name: string; 
  description?: string; 
  province?: string; 
  city?: string;
  type?: number;
  schoolId?: number;
  schoolIds?: number[];
}) {
  return post<ProjectVO>('/project', data)
}

/** 更新项目 */
export function updateProject(id: string, data: { name?: string; description?: string; province?: string; city?: string }) {
  return put<ProjectVO>(`/project/${id}`, data)
}

/** 管理员将用户加入项目（批量） */
export function addUsersToProject(projectId: string, userIds: string[]) {
  return post<void>(`/project/${projectId}/users`, userIds)
}

/** 老师加入项目 */
export function joinProject(projectId: string) {
  return post<void>(`/project/${projectId}/join`, null, {
    headers: { 'X-User-Id': '1' },
  })
}

/** 老师退出项目 */
export function leaveProject(projectId: string) {
  return del<void>(`/project/${projectId}/leave`, {
    headers: { 'X-User-Id': '1' },
  })
}

/** 删除项目 */
export function deleteProject(id: string) {
  return del<void>(`/project/${id}`)
}

// ==================== 配置管理 API ====================

/** 工具 VO（学员端查看项目工具） */
export interface ToolVO {
  code: string
  name: string
  description: string
  isEnabled: boolean
}

/** 查询项目下某角色可见的工具列表 */
export function listProjectTools(projectId: string, role: number = 3) {
  return get<ToolVO[]>(`/project/${projectId}/config/tools`, { role })
}

/** 查询项目下某角色的工具配置列表 */
export function listConfigs(projectId: string, role: number) {
  return get<ConfigVO[]>(`/project/${projectId}/config`, { role })
}

/** 更新单条配置 */
export function updateConfig(projectId: string, data: Record<string, any>) {
  return put<void>(`/project/${projectId}/config`, data)
}

/** 批量更新配置 */
export function batchUpdateConfigs(projectId: string, configs: Record<string, any>[]) {
  return put<void>(`/project/${projectId}/config/batch`, configs)
}
