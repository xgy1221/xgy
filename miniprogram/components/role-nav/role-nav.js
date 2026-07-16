const { ROLE_TABS } = require('../../utils/constants')
const auth = require('../../utils/auth')
const motion = require('../../utils/motion')

Component({
  properties: {
    current: {
      type: String,
      value: ''
    }
  },

  data: {
    tabs: []
  },

  lifetimes: {
    attached() {
      const role = auth.getCurrentRole()
      this.setData({
        tabs: ROLE_TABS[role] || []
      })
    }
  },

  methods: {
    onTap(e) {
      const path = e.currentTarget.dataset.path
      if (!path || path === this.data.current) return
      motion.tap('light')
      wx.redirectTo({ url: path })
    }
  }
})
