/**
 * 试卷服务 API — 对接 exam-project-service
 * 包含试卷管理、题目管理、答题等功能
 */
import { get, post, put, del } from './index'

// ==================== 类型定义 ====================

/** 试卷详情 VO */
export interface PaperVO {
  id: string
  title: string
  description: string
  paperType: number
  totalScore: number
  passScore: number
  questionCount: number
  durationMinutes: number
  status: number
  publisherName: string
  projectId: string
  projectName: string
  createdAt: string
  questions: QuestionVO[]
}

/** 题目 VO */
export interface QuestionVO {
  id: string
  paperId: string
  title: string
  questionType: number
  score: number
  isRequired: number
  sortOrder: number
  analysis: string
  options: OptionVO[]
}

/** 选项 VO */
export interface OptionVO {
  id: string
  optionLabel: string
  optionContent: string
  isCorrect: number
  sortOrder: number
}

/** 试卷列表分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 学员端试卷列表项 */
export interface MyPaperVO {
  responseId: string
  paperId: string
  paperTitle: string
  paperType: number
  totalScore: number
  passScore: number
  questionCount: number
  durationMinutes: number
  publisherName: string
  status: number
  score: number
  isPass: number
  submitTime: string
  reviewTime: string
}

/** 学员答题页试卷 VO */
export interface ExamPaperVO {
  paperId: string
  paperName: string
  totalScore: number
  passScore: number
  questionCount: number
  durationMinutes: number
  questions: ExamQuestionVO[]
}

/** 学员答题页题目 VO */
export interface ExamQuestionVO {
  questionId: string
  stem: string
  type: 'single' | 'multiple' | 'judge' | 'essay'
  score: number
  options: ExamOptionVO[]
}

/** 学员答题页选项 VO */
export interface ExamOptionVO {
  optionId: string
  optionKey: string
  content: string
}

// ==================== 试卷 API ====================

/** 创建试卷 */
export function createPaper(data: {
  title: string
  description?: string
  paperType?: number
  totalScore: number
  passScore: number
  durationMinutes: number
  projectId: string
}) {
  return post<PaperVO>('/paper', data)
}

/** 更新试卷信息 */
export function updatePaper(id: string, data: {
  title?: string
  description?: string
  paperType?: number
  totalScore?: number
  passScore?: number
  durationMinutes?: number
}) {
  return put<PaperVO>(`/paper/${id}`, data)
}

/** 查询项目下的试卷列表（支持名称和状态筛选） */
export function listPapers(projectId: string, params?: {
  name?: string
  status?: number
  page?: number
  size?: number
}) {
  return get<PageResult<PaperVO>>('/paper/list', {
    projectId,
    name: params?.name,
    status: params?.status,
    page: params?.page || 1,
    size: params?.size || 10
  })
}

/** 查询试卷详情 */
export function getPaperDetail(id: string) {
  return get<PaperVO>(`/paper/${id}`)
}

/** 获取学员答题页试卷数据（不含正确答案） */
export function getExamPaper(id: string) {
  return get<ExamPaperVO>(`/paper/${id}/exam`)
}

/** 添加题目到试卷 */
export function addQuestion(paperId: string, data: {
  title: string
  questionType: number
  score: number
  isRequired?: number
  sortOrder?: number
  analysis?: string
  options?: {
    optionLabel: string
    optionContent: string
    isCorrect?: number
    sortOrder?: number
  }[]
}) {
  return post<void>(`/paper/${paperId}/question`, data)
}

/** 更新题目 */
export function updateQuestion(paperId: string, questionId: string, data: {
  title: string
  questionType: number
  score: number
  isRequired?: number
  sortOrder?: number
  analysis?: string
  options?: {
    optionLabel: string
    optionContent: string
    isCorrect?: number
    sortOrder?: number
  }[]
}) {
  return put<void>(`/paper/${paperId}/question/${questionId}`, data)
}

/** 删除题目 */
export function deleteQuestion(paperId: string, questionId: string) {
  return del<void>(`/paper/${paperId}/question/${questionId}`)
}

/** 查询学员的试卷列表（从 response 表） */
export function listMyPapers(projectId: string) {
  return get<MyPaperVO[]>('/paper/my', { projectId })
}

/** 发布试卷 */
export function publishPaper(id: string) {
  return post<void>(`/paper/${id}/publish`)
}

/** 下线试卷 */
export function closePaper(id: string) {
  return post<void>(`/paper/${id}/close`)
}

/** 删除试卷 */
export function deletePaper(id: string) {
  return del<void>(`/paper/${id}`)
}

// ==================== 答题 API ====================

/** 提交结果 VO */
export interface SubmitResultVO {
  totalScore: number
  objectiveScore: number
  correctCount: number
  totalCount: number
  isPass: number
}

/** 开始答题 */
export function startExam(paperId: string) {
  return post<void>(`/answer/${paperId}/start`)
}

/** 保存答题进度到 Redis */
export function saveProgress(paperId: string, data: {
  answers: Record<string, string>
  remainingSeconds: number
}) {
  return post<void>(`/answer/${paperId}/save`, data)
}

/** 提交试卷 */
export function submitExam(paperId: string) {
  return post<SubmitResultVO>(`/answer/${paperId}/submit`)
}

// ==================== 成绩查询 API ====================

/** 成绩详情 — 选项 VO */
export interface ExamResultOptionVO {
  optionId: string
  optionLabel: string
  optionContent: string
  isCorrect: boolean
}

/** 成绩详情 — 逐题结果 VO */
export interface ExamResultQuestionVO {
  questionId: string
  stem: string
  questionType: number
  score: number
  sortOrder: number
  userAnswer: string
  gotScore: number
  isCorrect: boolean | null
  reviewComment: string | null
  options: ExamResultOptionVO[]
}

/** 成绩详情 — 试卷级 VO */
export interface ExamResultVO {
  paperName: string
  totalScore: number
  passScore: number
  userScore: number
  isPass: boolean
  questionCount: number
  correctCount: number
  objectiveCount: number
  submitTime: string
  reviewTime: string
  questions: ExamResultQuestionVO[]
}

/** 查询已批阅试卷的完整答卷结果 */
export function getExamResult(paperId: string) {
  return get<ExamResultVO>(`/answer/${paperId}/result`)
}
