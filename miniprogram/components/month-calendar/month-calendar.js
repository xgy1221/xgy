const { getMonthMatrix, shiftMonth, todayKey } = require('../../utils/date')

Component({
  properties: {
    selected: {
      type: String,
      value: ''
    },
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
      ;(list || []).forEach((d) => {
        markedMap[d] = true
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
      const next = shiftMonth(this.data.year, this.data.month, -1)
      this.setData(next, () => this.build())
    },
    onNext() {
      const next = shiftMonth(this.data.year, this.data.month, 1)
      this.setData(next, () => this.build())
    },
    onSelect(e) {
      const key = e.currentTarget.dataset.key
      if (!key) return
      this.triggerEvent('select', { date: key })
    }
  }
})
