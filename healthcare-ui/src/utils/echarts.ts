import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer
])

export { echarts }

/** 图表主题色板：青绿为主，辅以琥珀、玫红、天蓝、靛紫、柠檬。 */
export const CHART_COLORS = [
  '#0f8a7e',
  '#d98324',
  '#c8394a',
  '#2b7fd4',
  '#7a5ea8',
  '#8aa62f',
  '#3f7d8c',
  '#b8623f'
]

/** 通用坐标轴样式，与页面视觉保持一致。 */
const axisStyle = {
  axisLine: { lineStyle: { color: '#dfe5e7' } },
  axisTick: { show: false },
  axisLabel: { color: '#5c6b70', fontSize: 12 },
  splitLine: { lineStyle: { color: '#eef1f2', type: 'dashed' as const } }
}

/**
 * 在指定容器上初始化图表实例。
 */
export function initChart(el: HTMLElement | undefined | null) {
  if (!el) return undefined
  const existing = echarts.getInstanceByDom(el)
  if (existing) existing.dispose()
  return echarts.init(el)
}

/**
 * 折线图配置。
 */
export function lineOption(
  names: string[],
  series: { name: string; data: number[] }[]
) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'axis' },
    legend: { top: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    grid: { top: 42, left: 12, right: 16, bottom: 8, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: names, ...axisStyle },
    yAxis: { type: 'value', ...axisStyle },
    series: series.map((item) => ({
      name: item.name,
      type: 'line',
      smooth: true,
      symbolSize: 6,
      areaStyle: { opacity: 0.08 },
      data: item.data
    }))
  }
}

/**
 * 柱状图配置。
 */
export function barOption(
  names: string[],
  values: number[],
  seriesName = '数量'
) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 24, left: 12, right: 16, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: names, ...axisStyle, splitLine: { show: false } },
    yAxis: { type: 'value', ...axisStyle },
    series: [
      {
        name: seriesName,
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: { borderRadius: [3, 3, 0, 0] },
        data: values
      }
    ]
  }
}

/**
 * 饼图配置。
 */
export function pieOption(
  names: string[],
  values: number[],
  seriesName = '占比'
) {
  return {
    color: CHART_COLORS,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 10 },
    series: [
      {
        name: seriesName,
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: true,
        label: { show: false },
        data: names.map((name, index) => ({ name, value: values[index] }))
      }
    ]
  }
}

/**
 * 横向条形图配置，用于排行类展示。
 */
export function rankOption(names: string[], values: number[]) {
  const ordered = names
    .map((name, index) => ({ name, value: values[index] ?? 0 }))
    .sort((a, b) => a.value - b.value)
  return {
    color: ['#0f8a7e'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { top: 8, left: 12, right: 24, bottom: 8, containLabel: true },
    xAxis: { type: 'value', ...axisStyle },
    yAxis: {
      type: 'category',
      data: ordered.map((item) => item.name),
      ...axisStyle,
      splitLine: { show: false }
    },
    series: [
      {
        type: 'bar',
        barMaxWidth: 18,
        itemStyle: { borderRadius: [0, 3, 3, 0] },
        data: ordered.map((item) => item.value)
      }
    ]
  }
}

/**
 * 窗口尺寸变化时重绘图表。
 */
export function useChartResize(chart: { resize: () => void } | undefined) {
  if (!chart) return () => undefined
  const handler = () => chart.resize()
  window.addEventListener('resize', handler)
  return () => window.removeEventListener('resize', handler)
}
