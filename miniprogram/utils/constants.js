/** 角色定义与导航配置 */

const ROLES = {
  STUDENT: 'student',
  TEACHER: 'teacher',
  ACADEMIC: 'academic',
  PARTNER: 'partner',
  ADMIN: 'admin'
}

const ROLE_META = {
  [ROLES.STUDENT]: {
    key: ROLES.STUDENT,
    name: '学生 / 家长',
    shortName: '家长端',
    desc: '日历课表、报读进度、课后评价消课',
    color: '#0F3D3E',
    home: '/pages/student/home/home'
  },
  [ROLES.TEACHER]: {
    key: ROLES.TEACHER,
    name: '授课老师',
    shortName: '老师端',
    desc: '班级日历、临补插班、下课互评消课',
    color: '#1A5C5E',
    home: '/pages/teacher/home/home'
  },
  [ROLES.ACADEMIC]: {
    key: ROLES.ACADEMIC,
    name: '教务',
    shortName: '教务端',
    desc: '排课、报名、班级与课次管理',
    color: '#B45309',
    home: '/pages/academic/home/home'
  },
  [ROLES.PARTNER]: {
    key: ROLES.PARTNER,
    name: '合伙人',
    shortName: '合伙端',
    desc: '业绩看板、分润与团队概况',
    color: '#0E7490',
    home: '/pages/partner/home/home'
  },
  [ROLES.ADMIN]: {
    key: ROLES.ADMIN,
    name: '管理员',
    shortName: '管理端',
    desc: '账号权限、组织架构与系统设置',
    color: '#334155',
    home: '/pages/admin/home/home'
  }
}

const ROLE_TABS = {
  [ROLES.STUDENT]: [
    { pagePath: '/pages/student/home/home', text: '课表', icon: '▦' },
    { pagePath: '/pages/student/courses/courses', text: '课程', icon: '▣' },
    { pagePath: '/pages/student/schedule/schedule', text: '全部', icon: '☰' },
    { pagePath: '/pages/student/mine/mine', text: '我的', icon: '☺' }
  ],
  [ROLES.TEACHER]: [
    { pagePath: '/pages/teacher/home/home', text: '课表', icon: '▦' },
    { pagePath: '/pages/teacher/classes/classes', text: '班级', icon: '▣' },
    { pagePath: '/pages/teacher/students/students', text: '学员', icon: '☰' },
    { pagePath: '/pages/teacher/mine/mine', text: '我的', icon: '☺' }
  ],
  [ROLES.ACADEMIC]: [
    { pagePath: '/pages/academic/home/home', text: '首页', icon: '⌂' },
    { pagePath: '/pages/academic/courses/courses', text: '课程', icon: '▣' },
    { pagePath: '/pages/academic/schedule/schedule', text: '排课', icon: '▦' },
    { pagePath: '/pages/academic/enroll/enroll', text: '报名', icon: '✎' },
    { pagePath: '/pages/academic/mine/mine', text: '我的', icon: '☺' }
  ],
  [ROLES.PARTNER]: [
    { pagePath: '/pages/partner/home/home', text: '首页', icon: '⌂' },
    { pagePath: '/pages/partner/performance/performance', text: '业绩', icon: '▣' },
    { pagePath: '/pages/partner/team/team', text: '团队', icon: '▦' },
    { pagePath: '/pages/partner/mine/mine', text: '我的', icon: '☺' }
  ],
  [ROLES.ADMIN]: [
    { pagePath: '/pages/admin/home/home', text: '首页', icon: '⌂' },
    { pagePath: '/pages/admin/users/users', text: '账号', icon: '▣' },
    { pagePath: '/pages/admin/org/org', text: '组织', icon: '▦' },
    { pagePath: '/pages/admin/settings/settings', text: '设置', icon: '⚙' },
    { pagePath: '/pages/admin/mine/mine', text: '我的', icon: '☺' }
  ]
}

module.exports = {
  ROLES,
  ROLE_META,
  ROLE_TABS
}
