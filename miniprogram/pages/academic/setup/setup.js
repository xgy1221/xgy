const auth = require('../../../utils/auth')
const { ROLES } = require('../../../utils/constants')
const studentsService = require('../../../services/students')
const parentsService = require('../../../services/parents')
const packagesService = require('../../../services/packages')
const enrollmentsService = require('../../../services/enrollments')

Page({
  data: {
    roleLabel: '教务',
    sourceLabel: '教务代录',
    phone: '',
    parentName: '',
    familyStudents: [],
    studentId: '',
    form: {
      studentName: '',
      grade: '',
      campus: '',
      remark: ''
    },
    packages: [],
    currentEnrolls: []
  },

  onLoad(query) {
    if (!auth.requireAuth()) return
    const role = auth.getCurrentRole()
    const isTeacher = role === ROLES.TEACHER
    this.setData({
      roleLabel: isTeacher ? '老师' : '教务',
      sourceLabel: isTeacher ? '老师代录' : '教务代录',
      phone: query.phone || '',
      studentId: query.studentId || ''
    })

    if (query.phone) {
      this.loadFamily(query.phone, query.studentId)
    } else {
      this.resetPackages()
    }
  },

  onPhone(e) {
    this.setData({ phone: e.detail.value })
  },
  onParentName(e) {
    this.setData({ parentName: e.detail.value })
  },
  onForm(e) {
    this.setData({ [`form.${e.currentTarget.dataset.key}`]: e.detail.value })
  },

  onSearch() {
    const phone = (this.data.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请输入正确手机号', icon: 'none' })
      return
    }
    this.loadFamily(phone)
  },

  loadFamily(phone, preferredStudentId) {
    const orgId = auth.getCurrentOrgId()
    parentsService.ensureParentAccess(phone, '', orgId)
    const profile = parentsService.getParentProfile(phone) || {}
    // 只看本机构孩子，避免把家长在其他机构的学员拉进来改档
    const students = studentsService
      .getStudentsByPhone(phone)
      .filter((s) => s.orgId === orgId)
      .map((s) => ({
        ...s,
        enrollCount: enrollmentsService.getEnrollmentsByStudent(s.id).length
      }))

    let studentId = preferredStudentId || this.data.studentId
    if (studentId && !students.some((s) => s.id === studentId)) studentId = ''
    if (!studentId && students.length === 1) studentId = students[0].id

    const picked = students.find((s) => s.id === studentId)
    this.setData({
      phone,
      parentName: (picked && picked.parentName) || profile.parentName || '',
      familyStudents: students,
      studentId: studentId || '',
      form: picked
        ? {
            studentName: picked.studentName,
            grade: picked.grade || '',
            campus: picked.campus || '',
            remark: picked.remark || ''
          }
        : {
            studentName: '',
            grade: '',
            campus: '',
            remark: ''
          }
    })
    this.refreshPackagesAndEnrolls(studentId)
  },

  resetPackages(selected) {
    const selectedMap = selected || {}
    const orgId = auth.getCurrentOrgId()
    const packages = packagesService.getOnSalePackages(orgId).map((p) => ({
      ...p,
      checked: !!selectedMap[p.id],
      remainLessons:
        selectedMap[p.id] != null ? String(selectedMap[p.id]) : String(p.lessonCount)
    }))
    this.setData({ packages })
  },

  refreshPackagesAndEnrolls(studentId) {
    if (!studentId) {
      this.resetPackages()
      this.setData({ currentEnrolls: [] })
      return
    }
    const enrolls = enrollmentsService.getEnrollmentsDetailedByStudent(studentId)
    const selected = {}
    enrolls.forEach((e) => {
      selected[e.packageId] = e.remainLessons
    })
    this.resetPackages(selected)
    this.setData({ currentEnrolls: enrolls })
  },

  onPickStudent(e) {
    const id = e.currentTarget.dataset.id
    const stu = this.data.familyStudents.find((s) => s.id === id)
    if (!stu) return
    this.setData({
      studentId: id,
      parentName: stu.parentName || this.data.parentName,
      form: {
        studentName: stu.studentName,
        grade: stu.grade || '',
        campus: stu.campus || '',
        remark: stu.remark || ''
      }
    })
    this.refreshPackagesAndEnrolls(id)
  },

  onSaveStudent() {
    const phone = (this.data.phone || '').trim()
    if (!/^1\d{10}$/.test(phone)) {
      wx.showToast({ title: '请先填写家长手机号', icon: 'none' })
      return
    }
    const parentName = (this.data.parentName || '').trim() || '家长'
    const orgId = auth.getCurrentOrgId()
    if (!orgId) {
      wx.showToast({ title: '缺少机构上下文', icon: 'none' })
      return
    }
    parentsService.ensureParentAccess(phone, parentName, orgId)
    parentsService.ensureParentProfile(phone, { parentName })

    const result = studentsService.upsertStudent({
      ...this.data.form,
      orgId,
      parentPhone: phone,
      parentName
    })
    if (!result.ok) {
      wx.showToast({ title: result.message, icon: 'none' })
      return
    }

    wx.showToast({ title: result.created ? '学员已创建' : '学员已更新', icon: 'success' })
    this.loadFamily(phone, result.student.id)
  },

  onTogglePkg(e) {
    const id = e.currentTarget.dataset.id
    const checked = e.detail.value
    const packages = this.data.packages.map((p) => {
      if (p.id !== id) return p
      return {
        ...p,
        checked,
        remainLessons: p.remainLessons || String(p.lessonCount)
      }
    })
    this.setData({ packages })
  },

  onRemain(e) {
    const id = e.currentTarget.dataset.id
    const packages = this.data.packages.map((p) =>
      p.id === id ? { ...p, remainLessons: e.detail.value } : p
    )
    this.setData({ packages })
  },

  onSaveEnrolls() {
    if (!this.data.studentId) {
      wx.showToast({ title: '请先保存学员', icon: 'none' })
      return
    }
    const checked = this.data.packages.filter((p) => p.checked)
    if (!checked.length) {
      wx.showToast({ title: '请至少选择一个教案', icon: 'none' })
      return
    }

    checked.forEach((p) => {
      enrollmentsService.upsertEnrollment({
        studentId: this.data.studentId,
        packageId: p.id,
        remainLessons: p.remainLessons,
        totalLessons: p.lessonCount,
        status: '学习中',
        source: this.data.sourceLabel,
        overwrite: true
      })
    })

    wx.showToast({ title: '报读已保存', icon: 'success' })
    this.refreshPackagesAndEnrolls(this.data.studentId)
    this.loadFamily(this.data.phone, this.data.studentId)
  }
})
