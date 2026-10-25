import request from './request'
import type { PageResult } from './system'

/* 患者 */
export const pagePatients = (params: any) =>
  request.get<any, PageResult<any>>('/patient/page', { params })
export const getPatient = (id: number) => request.get(`/patient/${id}`)
export const patientSummary = (id: number) => request.get(`/patient/${id}/summary`)
export const createPatient = (data: any) => request.post('/patient', data)
export const updatePatient = (id: number, data: any) => request.put(`/patient/${id}`, data)
export const deletePatient = (id: number) => request.delete(`/patient/${id}`)
export const patientRecords = (id: number) => request.get(`/patient/${id}/records`)
export const savePatientRecord = (data: any) => request.post('/patient/record', data)
export const listPatientCards = (patientId: number) =>
  request.get('/patient/card/list', { params: { patientId } })
export const issuePatientCard = (data: any) => request.post('/patient/card', data)
export const reissuePatientCard = (data: any) => request.post('/patient/card/reissue', data)
export const losePatientCard = (id: number) => request.put(`/patient/card/${id}/loss`)
/** 注销就诊卡：余额退回、卡号作废，不可撤销 */
export const cancelPatientCard = (id: number, reason?: string) =>
  request.put(`/patient/card/${id}/cancel`, null, { params: { reason } })
export const pageFollowUps = (params: any) => request.get('/patient/followup/page', { params })
export const saveFollowUp = (data: any) => request.post('/patient/followup', data)
export const pendingFollowUps = () => request.get('/patient/followup/pending')

/* 挂号与预约 */
export const listSchedules = (params: any) => request.get('/registration/schedule', { params })
export const saveSchedule = (data: any) => request.post('/registration/schedule', data)
export const updateSchedule = (id: number, data: any) => request.put(`/registration/schedule/${id}`, data)
export const stopSchedule = (id: number) => request.put(`/registration/schedule/${id}/stop`)
export const register = (params: any) => request.post('/registration', null, { params })
export const cancelRegistration = (id: number, reason?: string) =>
  request.put(`/registration/${id}/cancel`, null, { params: { reason } })
/** 候诊队列：不传 deptId = 全部科室（大屏默认），医生视角由后端按"仅本人"收窄。 */
export const registrationQueue = (deptId?: number, doctorId?: number) =>
  request.get('/registration/queue', {
    params: {
      ...(deptId ? { deptId } : {}),
      ...(doctorId ? { doctorId } : {})
    }
  })
export const callNumber = (id: number) => request.post(`/registration/call/${id}`)
/** 过号：叫了没来，把号退回候诊队列 */
export const passCall = (id: number) => request.put(`/registration/call/${id}/pass`)
/** 完成叫号：患者已就诊 */
export const finishCall = (id: number) => request.put(`/registration/call/${id}/finish`)
export const pageRegistrations = (params: any) =>
  request.get<any, PageResult<any>>('/registration/page', { params })
export const completeVisit = (id: number) => request.put(`/registration/${id}/complete`)
export const pageAppointments = (params: any) =>
  request.get<any, PageResult<any>>('/appointment/page', { params })
export const createAppointment = (data: any) => request.post('/appointment', data)
export const cancelAppointment = (id: number, reason?: string) =>
  request.put(`/appointment/${id}/cancel`, null, { params: { reason } })
export const noShowAppointment = (id: number) => request.put(`/appointment/${id}/noshow`)
/** 取号：预约转正式挂号，复用预约已占的号源，不重复扣号 */
export const checkinAppointment = (id: number, payType?: number) =>
  request.post(`/appointment/${id}/checkin`, null, { params: { payType } })

/* 门诊 */
export const listVisits = (params: any) => request.get('/clinic/visit/list', { params })
export const receiveVisit = (id: number) => request.post(`/clinic/visit/${id}/receive`)
export const saveClinicRecord = (data: any) => request.post('/clinic/record', data)
export const submitClinicRecord = (id: number) => request.put(`/clinic/record/${id}/submit`)
/** 归档病历：归档后不可再修改 */
export const archiveClinicRecord = (id: number) => request.put(`/clinic/record/${id}/archive`)
export const pageClinicRecords = (params: any) =>
  request.get<any, PageResult<any>>('/clinic/record/page', { params })
export const pagePrescriptions = (params: any) =>
  request.get<any, PageResult<any>>('/clinic/prescription/page', { params })
export const createPrescription = (data: any) => request.post('/clinic/prescription', data)
export const getPrescription = (id: number) => request.get(`/clinic/prescription/${id}`)
export const voidPrescription = (id: number) => request.delete(`/clinic/prescription/${id}`)

/* 住院 */
export const listBeds = (params: any) => request.get('/inpatient/bed/list', { params })
export const saveBed = (data: any) => request.post('/inpatient/bed', data)
export const updateBed = (id: number, data: any) => request.put(`/inpatient/bed/${id}`, data)
/** 床位转维修：占用中的床位不能直接转 */
export const maintainBed = (id: number) => request.put(`/inpatient/bed/${id}/maintain`)
/** 维修完成，床位恢复可用 */
export const recoverBed = (id: number) => request.put(`/inpatient/bed/${id}/recover`)
export const createAdmission = (data: any) => request.post('/inpatient/admission', data)
export const pageAdmissions = (params: any) =>
  request.get<any, PageResult<any>>('/inpatient/admission/page', { params })
export const dischargeAdmission = (id: number) => request.put(`/inpatient/admission/${id}/discharge`)
export const transferAdmission = (id: number, params: any) =>
  request.post(`/inpatient/admission/${id}/transfer`, null, { params })
export const pageOrders = (params: any) => request.get('/inpatient/order/page', { params })
export const createOrder = (data: any) => request.post('/inpatient/order', data)
export const executeOrder = (id: number, result?: string) =>
  request.put(`/inpatient/order/${id}/execute`, null, { params: { result } })
export const stopOrder = (id: number) => request.put(`/inpatient/order/${id}/stop`)
export const finishOrder = (id: number) => request.put(`/inpatient/order/${id}/finish`)
export const addNursingRecord = (data: any) => request.post('/inpatient/nursing', data)
export const pageNursingRecords = (params: any) =>
  request.get<any, PageResult<any>>('/inpatient/nursing/page', { params })
export const addCourseRecord = (data: any) => request.post('/inpatient/course', data)
export const pageCourseRecords = (params: any) =>
  request.get<any, PageResult<any>>('/inpatient/course/page', { params })
export const settleAdmission = (params: any) => request.post('/inpatient/settlement', null, { params })
export const pageSettlements = (params: any) =>
  request.get<any, PageResult<any>>('/inpatient/settlement/page', { params })
/** 结算退费：仅已结算的单据可退，退费后状态转 2 并留痕 */
export const refundSettlement = (id: number, reason?: string) =>
  request.put(`/inpatient/settlement/${id}/refund`, null, { params: { reason } })

/* 公共卫生 */
export const pageHealthRecords = (params: any) =>
  request.get<any, PageResult<any>>('/health/record/page', { params })
export const saveHealthRecord = (data: any) => request.post('/health/record', data)
export const updateHealthRecord = (id: number, data: any) => request.put(`/health/record/${id}`, data)
export const deleteHealthRecord = (id: number) => request.delete(`/health/record/${id}`)
export const pageVaccinations = (params: any) =>
  request.get<any, PageResult<any>>('/health/vaccination/page', { params })
export const createVaccination = (data: any) => request.post('/health/vaccination', data)
export const vaccinationReminders = (params: any) =>
  request.get<any, PageResult<any>>('/health/vaccination/remind', { params })
export const pageInfectious = (params: any) =>
  request.get<any, PageResult<any>>('/health/infectious/page', { params })
export const reportInfectious = (data: any) => request.post('/health/infectious/report', data)
/** 提交上报：0 待上报 → 1 已上报 */
export const submitInfectious = (id: number) => request.put(`/health/infectious/${id}/submit`)
/** 审核报卡：1 已上报 → 2 已审核 */
export const auditInfectious = (id: number, remark?: string) =>
  request.put(`/health/infectious/${id}/audit`, null, { params: { remark } })
export const pageCheckups = (params: any) =>
  request.get<any, PageResult<any>>('/health/checkup/page', { params })
export const createCheckup = (data: any) => request.post('/health/checkup', data)
