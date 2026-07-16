const auth = require('../../utils/auth')
const studentsService = require('../../services/students')
const parentsService = require('../../services/parents')
const packagesService = require('../../services/packages')
const enrollmentsService = require('../../services/enrollments')
const orgs = require('../../services/orgs')

Page({
  data: {
    step: 1,
    phone: '',
    parentName: '',
    isNewParent: false,
    orgOptions: [],
    selectedOrgId: '',
    selectedOrgName: '',
    form: {
      studentName: '',
      grade: '',
      campus: '',
      remark: ''
    },
    students: [],
    packages: [],
    activeStudentId: '',
    activeStudentName: '',
    selectedMap: {}
  },

  onLoad(query) {
    if (!auth.requireAuth()) return
    const session = auth.getSession()
    const phone = session.user.phone
    const profile = parentsService.getParentProfile(phone) || {}
    const students = studentsService.getStudentsByPhone(phone)
    const orgOptions = orgs.listOrgs()
    const whitelistOrgs = parentsService.listOrgIdsForPhone(phone)
    let selectedOrgId =
      auth.getCurrentOrgId() ||
      (students[0] && students[0].orgId) ||
      (whitelistOrgs.length === 1 ? whitelistOrgs[0] : '') ||
      (orgOptions[0] && orgOptions[0].id) ||
      ''

    const activeStudentId = students[0] ? students[0].id : ''
    let step = 1
    if (query && query.mode === 'packages' && students.length) step = 3
    else if (students.length) step = 2

    if (selectedOrgId) auth.setCurrentOrgId(selectedOrgId)

    this.setData(
      {
        phone,
        parentName: profile.parentName || session.user.name || '',
        isNewParent: !!session.isNewParent,
        orgOptions,
        selectedOrgId,
        selectedOrgName: orgs.getOrgName(selectedOrgId),
        students,
        activeStudentId,
        activeStudentName: students[0] ? students[0].studentName : '',
        step,
        packages: this.buildPackages(selectedOrgId, activeStudentId, {})
      },
      () => {
        if (step === 3 && activeStudentId) {
          this.syncPackageChecks(activeStudentId)
        }
      }
    )
  },

  buildPackages(orgId, studentId, selectedMap) {
    const selected = (selectedMap && selectedMap[studentId]) || []
    return packagesService.getOnSalePackages(orgId).map((p) => ({
      ...p,
      outlineText: (p.outline || []).slice(0, 3).join(' / '),
      checked: selected.indexOf(p.id) >= 0
    }))
  },

  onParentName(e) {
    this.setData({ parentName: e.detail.value })
  },

  onPickOrg(e) {
    const orgId = e.currentTarget.dataset.id
    if (!orgId) return
    auth.setCurrentOrgId(orgId)
    parentsService.ensureParentAccess(this.data.phone, this.data.parentName, orgId)
    this.setData({
      selectedOrgId: orgId,
      selectedOrgName: orgs.getOrgName(orgId),
      packages: this.buildPackages(orgId, this.data.activeStudentId, this.data.selectedMap)
    })
  },

  goStep2() {
    if (!this.data.selectedOrgId) {
      wx.showToast({ title: '请先选择机构', icon: 'none' })
      return
    }
    const parentName = (this.data.parentName || '').trim() || '家长'
    parentsService.ensureParentProfile(this.data.phone, { parentName })
    parentsService.ensureParentAccess(this.data.phone, parentName, this.data.selectedOrgId)
    const session = auth.getSession()
    session.user.name = parentName
    session.user.avatarText = parentName.slice(0, 1)
    auth.setSession(session)
    auth.setCurrentOrgId(this.data.selectedOrgId)
    this.setData({ parentName, step: 2 })
  },

  onForm(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ [`form.${key}`]: e.detail.value })
  },

  onAddStudent() {
    if (!this.data.selectedOrgId) {
      wx.showToast({ title: '请先选择机构', icon: 'none' })
      return
    }
    const result = studentsService.upsertStudent({
      ...this.data.form,
      orgId: this.data.selectedOrgId,
      parentPhone: this.data.phone,
      parentName: this.data.parentName
    })
    if (!result.ok) {
      wx.showToast({ title: result.message, icon: 'none' })
      return
    }

    const students = studentsService.getStudentsByPhone(this.data.phone)
    const session = auth.getSession()
    if (!session.currentStudentId) {
      auth.setCurrentStudentId(result.student.id)
    }
    session.needsOnboarding = false
    auth.setSession(session)

    this.setData({
      students,
      form: { studentName: '', grade: '', campus: '', remark: '' },
      activeStudentId: result.student.id,
      activeStudentName: result.student.studentName,
      packages: this.buildPackages(this.data.selectedOrgId, result.student.id, this.data.selectedMap)
    })
    wx.showToast({ title: result.created ? '已添加' : '已更新', icon: 'success' })
  },

  goStep3() {
    if (!this.data.students.length) {
      wx.showToast({ title: '请先添加学员', icon: 'none' })
      return
    }
    const sid = this.data.activeStudentId || this.data.students[0].id
    const stu = this.data.students.find((s) => s.id === sid)
    const orgId = (stu && stu.orgId) || this.data.selectedOrgId
    this.setData({
      selectedOrgId: orgId,
      selectedOrgName: orgs.getOrgName(orgId),
      packages: this.buildPackages(orgId, sid, this.data.selectedMap),
      step: 3
    })
    this.syncPackageChecks(sid)
  },

  onPickStudent(e) {
    const id = e.currentTarget.dataset.id
    this.syncPackageChecks(id)
  },

  syncPackageChecks(studentId) {
    const student = this.data.students.find((s) => s.id === studentId)
    const orgId = (student && student.orgId) || this.data.selectedOrgId
    const selected = this.data.selectedMap[studentId] || []
    const packages = this.buildPackages(orgId, studentId, this.data.selectedMap).map((p) => ({
      ...p,
      checked: selected.indexOf(p.id) >= 0
    }))
    this.setData({
      activeStudentId: studentId,
      activeStudentName: student ? student.studentName : '',
      selectedOrgId: orgId,
      selectedOrgName: orgs.getOrgName(orgId),
      packages
    })
  },

  onTogglePkg(e) {
    const id = e.currentTarget.dataset.id
    const studentId = this.data.activeStudentId
    const selectedMap = { ...this.data.selectedMap }
    const current = (selectedMap[studentId] || []).slice()
    const idx = current.indexOf(id)
    if (idx >= 0) current.splice(idx, 1)
    else current.push(id)
    selectedMap[studentId] = current
    this.setData({ selectedMap })
    this.syncPackageChecks(studentId)
  },

  onSavePackages() {
    const { selectedMap, students } = this.data
    students.forEach((stu) => {
      const ids = selectedMap[stu.id] || []
      if (ids.length) {
        enrollmentsService.enrollPackagesForStudent(stu.id, ids, '家长自选')
      }
    })
    this.finish()
  },

  onSkipPackages() {
    this.finish()
  },

  finish() {
    const phone = this.data.phone
    parentsService.markOnboarded(phone)
    const students = studentsService.getStudentsByPhone(phone)
    if (students.length) {
      auth.setCurrentStudentId(students[0].id)
    }
    const session = auth.getSession()
    session.needsOnboarding = false
    auth.setSession(session)
    wx.reLaunch({ url: '/pages/student/home/home' })
  }
})
