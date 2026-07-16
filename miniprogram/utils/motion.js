/**
 * 轻量交互反馈：震动 + 内容切换动画时机
 * 不引入重型动画库，适配微信小程序性能。
 */

function tap(type = 'light') {
  try {
    if (wx.vibrateShort) {
      wx.vibrateShort({ type })
    }
  } catch (e) {
    // ignore
  }
}

/**
 * 先淡出再写入数据再淡入，避免切孩子/切 Tab 时内容「硬切」。
 * @param {WechatMiniprogram.Page.Instance} page
 * @param {Function} apply 同步写入新数据的函数
 * @param {number} outMs
 */
function swap(page, apply, outMs = 120) {
  if (!page || typeof apply !== 'function') return Promise.resolve()
  page.setData({ contentReady: false })
  return new Promise((resolve) => {
    setTimeout(() => {
      apply()
      page.setData({ contentReady: true })
      resolve()
    }, outMs)
  })
}

function readyIn(page, delay = 16) {
  if (!page) return
  page.setData({ contentReady: false })
  setTimeout(() => page.setData({ contentReady: true }), delay)
}

module.exports = {
  tap,
  swap,
  readyIn
}
