const auth = require('../../utils/auth')
const studentsService = require('../../services/students')
const parentsService = require('../../services/parents')
const packagesService = require('../../services/packages')
const enrollmentsService = require('../../services/enrollments')

Page({
  data: {
    step: 1,
    phone: '',
    parentName: '',
    isNewParent: false,
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
    const packages = packagesService.getOnSalePackages().map((p) => ({
      ...p,
      outlineText: (p.outline || []).slice(0, 3).join(' / '),
      checked: false
    }))

    const activeStudentId = students[0] ? students[0].id : ''
    let step = 1
    if (query && query.mode === 'packages' && students.length) step = 3
    else if (students.length) step = 2

    this.setData(
      {
        phone,
        parentName: profile.parentName || session.user.name || '',
        isNewParent: !!session.isNewParent,
        students,
        packages,
        activeStudentId,
        activeStudentName: students[0] ? students[0].studentName : '',
        step
      },
      () => {
        if (step === 3 && activeStudentId) {
          this.syncPackageChecks(activeStudentId)
        }
      }
    )
  },

  onParentName(e) {
    this.setData({ parentName: e.detail.value })
  },

  goStep2() {
    const parentName = (this.data.parentName || '').trim() || '家长'
    parentsService.ensureParentProfile(this.data.phone, { parentName })
    const session = auth.getSession()
    session.user.name = parentName
    session.user.avatarText = parentName.slice(0, 1)
    auth.setSession(session)
    this.setData({ parentName, step: 2 })
  },

  onForm(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ [`form.${key}`]: e.detail.value })
  },

  onAddStudent() {
    const result = studentsService.upsertStudent({
      ...this.data.form,
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
      activeStudentName: result.student.studentName
    })
    wx.showToast({ title: result.created ? '已添加' : '已更新', icon: 'success' })
  },

  goStep3() {
    if (!this.data.students.length) {
      wx.showToast({ title: '请先添加学员', icon: 'none' })
      return
    }
    this.syncPackageChecks(this.data.activeStudentId || this.data.students[0].id)
    this.setData({ step: 3 })
  },

  onPickStudent(e) {
    const id = e.currentTarget.dataset.id
    this.syncPackageChecks(id)
  },

  syncPackageChecks(studentId) {
    const student = this.data.students.find((s) => s.id === studentId)
    const selected = this.data.selectedMap[studentId] || []
    const packages = this.data.packages.map((p) => ({
      ...p,
      checked: selected.indexOf(p.id) >= 0
    }))
    this.setData({
      activeStudentId: studentId,
      activeStudentName: student ? student.studentName : '',
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
