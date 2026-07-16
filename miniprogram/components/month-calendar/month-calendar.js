const { getMonthMatrix, shiftMonth, todayKey } = require('../../utils/date')
const motion = require('../../utils/motion')

Component({
  properties: {
    selected: {
      type: String,
      value: ''
    },
    /** [{ date, state: 'finished'|'upcoming'|'mixed' }] 或旧版字符串数组 */
    markedDates: {
      type: Array,
      value: []
    }
  },

  data: {
    year: 2026,
    month: 7,
    weeks: [],
    weekLabels: ['日', '一', '二', '三', '四', '五', '六'],
    today: todayKey(),
    markedMap: {}
  },

  observers: {
    markedDates(list) {
      const markedMap = {}
      ;(list || []).forEach((item) => {
        if (typeof item === 'string') {
          markedMap[item] = 'upcoming'
          return
        }
        if (item && item.date) {
          markedMap[item.date] = item.state || 'upcoming'
        }
      })
      this.setData({ markedMap })
    },
    selected(key) {
      if (!key) return
      const parts = key.split('-').map(Number)
      if (parts[0] !== this.data.year || parts[1] !== this.data.month) {
        this.setData({ year: parts[0], month: parts[1] }, () => this.build())
      }
    }
  },

  lifetimes: {
    attached() {
      const key = this.data.selected || todayKey()
      const parts = key.split('-').map(Number)
      this.setData({ year: parts[0], month: parts[1], today: todayKey() }, () => this.build())
    }
  },

  methods: {
    build() {
      this.setData({
        weeks: getMonthMatrix(this.data.year, this.data.month)
      })
    },
    onPrev() {
      motion.tap('light')
      const next = shiftMonth(this.data.year, this.data.month, -1)
      this.setData(next, () => this.build())
    },
    onNext() {
      motion.tap('light')
      const next = shiftMonth(this.data.year, this.data.month, 1)
      this.setData(next, () => this.build())
    },
    onSelect(e) {
      const key = e.currentTarget.dataset.key
      if (!key) return
      motion.tap('light')
      this.triggerEvent('select', { date: key })
    }
  }
})
