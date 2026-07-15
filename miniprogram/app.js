const auth = require('./utils/auth')

App({
  globalData: {
    session: null,
    brandName: '学管云'
  },

  onLaunch() {
    this.globalData.session = auth.getSession()
  }
})
