<template>
  <div class="page">
    <!-- ===== 炉型参数卡 + 燃烧工况 ===== -->
    <section id="sec-cards" class="wp-section">
      <h3 class="wp-section-title">焚烧炉炉型参数卡与燃烧工况（850℃ 达标线 / 烟气停留 2s）</h3>
      <div class="furnace-grid">
        <div
          v-for="f in furnaces"
          :key="f.furnaceCode"
          class="furnace-card"
          :class="{ bad: !f.combustionQualified, offline: f.combustionStatus === 'SHUTDOWN' }"
        >
          <div class="card-head">
            <div class="head-left">
              <span class="fcode mono">{{ f.furnaceCode }}</span>
              <span class="wp-tag primary">{{ f.furnaceTypeName }}</span>
              <span class="wp-tag" :class="statusTag(f)"><span class="wp-dot" :class="statusDot(f)"></span>{{ f.combustionStatusName }}</span>
              <span v-if="frozenSet.has(f.furnaceCode)" class="wp-tag danger frozen-badge">⛔ 投料冻结 · 异常处置中</span>
            </div>
            <div class="head-right mono">{{ f.furnaceName }}</div>
          </div>

          <div class="temp-block">
            <div class="temp-read">
              <div class="temp-value num" :class="{ danger: !f.combustionQualified }">{{ num(f.furnaceTempC, 1) }}</div>
              <div class="temp-unit">℃ 炉膛温度</div>
              <div class="temp-meta mono">停留 {{ num(f.flueGasResidenceS, 2) }} s · CO {{ num(f.coPpm, 0) }} ppm</div>
            </div>
            <div class="temp-col" :title="'额定 ' + f.ratedCapacityTpd + ' t/d'">
              <div
                class="temp-fill"
                :class="{ danger: !f.combustionQualified, glow: !f.combustionQualified }"
                :style="{ height: tempHeight(f) }"
              ></div>
              <div class="temp-line"><span>850℃ 达标线</span></div>
            </div>
          </div>

          <div class="unqualified" v-if="!f.combustionQualified">
            <span class="wp-tag danger">未达标</span>
            <span v-for="item in f.unqualifiedItems" :key="item" class="uq-item">{{ item }}</span>
          </div>

          <!-- 按炉型显示不同的专属参数 -->
          <div class="params">
            <div v-for="(val, key) in f.extraParams" :key="key" class="param">
              <span class="p-label">{{ key }}</span>
              <span class="p-value num">{{ val }}</span>
            </div>
          </div>

          <div class="card-foot">
            <span class="foot-item">额定 <b class="num">{{ num(f.ratedCapacityTpd, 0) }}</b> t/d</span>
            <span class="foot-item">当日入炉 <b class="num">{{ num(f.dailyProcessTon, 1) }}</b> t</span>
            <span class="foot-item">运行 <b class="num">{{ num(f.runningHours, 0) }}</b> h</span>
            <span class="foot-item">抓斗 <b class="mono">{{ f.grabNo || '--' }}</b></span>
          </div>

          <div class="actions">
            <el-button size="small" type="success" plain @click="run(f, 'START')">启炉</el-button>
            <el-button size="small" type="primary" @click="run(f, 'STABILIZE')">稳燃</el-button>
            <el-button size="small" type="warning" plain @click="run(f, 'STOP')">停炉</el-button>
            <el-button size="small" @click="judge(f)">达标判定</el-button>
            <el-button size="small" type="danger" plain @click="goDisposal(f)">处置单</el-button>
          </div>
          <div v-if="frozenSet.has(f.furnaceCode)" class="freeze-tip">
            该炉存在未恢复的异常工况处置单，抓斗投料已联锁冻结；达标判定不会自动解冻，须由另一名复核人员复核恢复。
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 炉型统计 ===== -->
    <section id="sec-typestat" class="wp-section">
      <h3 class="wp-section-title">按炉型（子类）统计 · 单表继承 TYPE() 查询</h3>
      <div class="type-grid">
        <div v-for="s in typeStats" :key="s.furnaceType" class="wp-panel type-card">
          <div class="wp-panel-body">
            <div class="type-name"><span class="wp-tag primary">{{ s.furnaceTypeName }}</span><span class="mono code">{{ s.furnaceType }}</span></div>
            <div class="type-row"><span>炉台数</span><b class="num">{{ s.furnaceCount }}</b></div>
            <div class="type-row"><span>额定处理能力</span><b class="num">{{ num(s.ratedCapacityTpd, 0) }} t/d</b></div>
            <div class="type-row"><span>当日入炉量</span><b class="num">{{ num(s.dailyProcessTon, 1) }} t</b></div>
            <div class="type-row"><span>平均炉膛温度</span><b class="num" :class="{ danger: s.avgFurnaceTempC < 850 }">{{ num(s.avgFurnaceTempC, 1) }} ℃</b></div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 余热锅炉 ===== -->
    <section id="sec-boiler" class="wp-section">
      <h3 class="wp-section-title">余热锅炉蒸汽工况</h3>
      <div class="boiler-grid">
        <div v-for="b in boilers" :key="b.boilerNo" class="wp-panel unit-card">
          <div class="wp-panel-title">
            <span>{{ b.boilerNo }} {{ b.boilerName }}</span>
            <span class="sub wp-tag" :class="b.status === 'RUNNING' ? 'ok' : 'primary'">{{ b.statusName }}</span>
          </div>
          <div class="wp-panel-body">
            <div class="kv-grid">
              <div class="kv"><span>蒸汽压力</span><b class="num">{{ num(b.steamPressureMpa, 2) }} MPa</b></div>
              <div class="kv"><span>蒸汽温度</span><b class="num">{{ num(b.steamTempC, 1) }} ℃</b></div>
              <div class="kv"><span>蒸发量</span><b class="num">{{ num(b.evaporationTph, 2) }} t/h</b></div>
              <div class="kv"><span>给水温度</span><b class="num">{{ num(b.feedWaterTempC, 1) }} ℃</b></div>
              <div class="kv"><span>排烟温度</span><b class="num">{{ num(b.flueGasTempC, 1) }} ℃</b></div>
              <div class="kv"><span>对应炉号</span><b class="mono">{{ b.furnaceCode }}</b></div>
            </div>
            <div class="load-row">
              <span class="load-label">蒸发负荷</span>
              <div class="wp-bar"><i :style="{ width: pct(b.loadRate) }"></i></div>
              <span class="num">{{ pct(b.loadRate) }}</span>
            </div>
            <div class="unit-actions">
              <el-button size="small" type="primary" plain @click="openBoiler(b)">调整工况</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 汽轮发电机组 ===== -->
    <section id="sec-turbine" class="wp-section">
      <h3 class="wp-section-title">汽轮发电机组 · 日结按相邻表码增量（表码≠单日发电量）</h3>
      <div class="turbine-grid">
        <div v-for="t in turbines" :key="t.unitNo" class="wp-panel unit-card" :class="{ held: t.meterStatus === 'METER_HELD' }">
          <div class="wp-panel-title">
            <span>{{ t.unitNo }} {{ t.unitName }}</span>
            <span class="sub wp-tag ok">{{ t.statusName }}</span>
          </div>
          <div class="wp-panel-body">
            <div class="meter-badges">
              <span class="wp-tag" :class="t.meterStatus === 'METER_HELD' ? 'danger' : 'ok'">
                {{ t.meterStatusName }}
              </span>
              <span class="wp-tag primary">基数来源：{{ t.meterSourceName }}</span>
              <span v-if="t.abnormalStatus && t.abnormalStatus !== 'NORMAL'" class="wp-tag warn">
                ⚠ {{ t.abnormalStatusName }}
              </span>
            </div>
            <div class="kv-grid">
              <div class="kv"><span>当前表码（累计）</span><b class="num">{{ num(t.meterReadingKwh, 0) }} kWh</b></div>
              <div class="kv"><span>上网表当前表码</span><b class="num">{{ num(t.gridMeterReadingKwh, 0) }} kWh</b></div>
              <div class="kv"><span>当日发电增量</span><b class="num hl">{{ num(t.generationKwh, 0) }} kWh</b></div>
              <div class="kv"><span>当日上网电量</span><b class="num hl">{{ num(t.gridKwh, 0) }} kWh</b></div>
              <div class="kv"><span>厂用电率</span><b class="num">{{ num(t.auxiliaryRate, 2) }} %</b></div>
              <div class="kv"><span>日结运行小时</span><b class="num">{{ num(t.runningHours, 1) }} h</b></div>
              <div class="kv"><span>最近日结日</span><b class="mono">{{ t.statDate || '--' }}</b></div>
              <div class="kv"><span>装机容量</span><b class="num">{{ num(t.capacityMw, 2) }} MW</b></div>
            </div>
            <div class="limit-row">
              <span>当日理论上限（装机 × 运行小时）</span>
              <b class="num">{{ num(t.theoreticalMaxKwh, 0) }} kWh · 负荷 {{ pct(t.capacityFactor) }}</b>
            </div>
            <div v-if="t.meterStatus === 'METER_HELD'" class="freeze-tip">
              ⛔ 该机组已暂停日结等待校表：校表确认新基数前不能日结，也不会把累计读数误算成发电量。
            </div>
            <div class="unit-actions">
              <el-button size="small" type="primary" :disabled="t.meterStatus === 'METER_HELD'" @click="openSettle(t)">日结登记</el-button>
              <el-button size="small" type="warning" plain @click="openMeterEvent(t)">表计异常处置</el-button>
              <el-button v-if="t.meterStatus === 'METER_HELD'" size="small" type="success" @click="openRelease(t)">校表恢复</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ===== 发电日结台账 ===== -->
    <section id="sec-daily" class="wp-section">
      <h3 class="wp-section-title">发电日结台账（每日计算依据：基数 / 期末表码 / 增量 / 基数来源 / 异常状态）</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table daily-table">
            <thead>
              <tr>
                <th>机组</th>
                <th>统计日</th>
                <th class="right">期初基数 kWh</th>
                <th class="right">期末表码 kWh</th>
                <th class="right">当日发电增量 kWh</th>
                <th class="right">当日上网电量 kWh</th>
                <th class="right">厂用电率 %</th>
                <th class="right">运行 h</th>
                <th>基数来源</th>
                <th>异常状态</th>
                <th>计算依据 / 留痕</th>
                <th>操作人</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="d in dailies" :key="d.id" :class="{ abnormal: d.abnormalStatus !== 'NORMAL' }">
                <td class="mono strong">{{ d.unitNo }}</td>
                <td class="mono">{{ d.statDate }}</td>
                <td class="right num">{{ num(d.baseReadingKwh, 0) }}</td>
                <td class="right num">{{ num(d.closingReadingKwh, 0) }}</td>
                <td class="right num hl">{{ num(d.generationKwh, 0) }}</td>
                <td class="right num">{{ num(d.gridKwh, 0) }}</td>
                <td class="right num">{{ num(d.auxiliaryRate, 2) }}</td>
                <td class="right num">{{ num(d.runningHours, 1) }}</td>
                <td>{{ d.baseTypeName }}</td>
                <td>
                  <span class="wp-tag" :class="d.abnormalStatus === 'NORMAL' ? 'ok' : 'warn'">{{ d.abnormalStatusName }}</span>
                </td>
                <td class="dim basis">{{ d.remark || '--' }}</td>
                <td>{{ d.operator || '--' }}</td>
              </tr>
              <tr v-if="!dailies.length"><td colspan="12" class="empty">暂无日结记录</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- ===== 月度处理量与发电量 ===== -->
    <section id="sec-monthly" class="wp-section">
      <h3 class="wp-section-title">按炉号统计月度处理量与发电量</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table">
            <thead>
              <tr>
                <th>炉号</th>
                <th>月份</th>
                <th class="right">处理量 t</th>
                <th class="right">发电量 kWh</th>
                <th class="right">记录条数</th>
                <th style="width: 200px">处理量占比</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(m, i) in monthly" :key="i">
                <td class="mono strong">{{ m.furnaceCode }}</td>
                <td class="mono">{{ m.periodMonth }}</td>
                <td class="right num">{{ num(m.processTon, 1) }}</td>
                <td class="right num">{{ num(m.powerKwh, 0) }}</td>
                <td class="right num">{{ m.shifts }}</td>
                <td><div class="wp-bar"><i :style="{ width: pct((m.processTon || 0) / maxMonthly) }"></i></div></td>
              </tr>
              <tr v-if="!monthly.length"><td colspan="6" class="empty">暂无月度数据</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- ===== 启停炉记录 ===== -->
    <section id="sec-logs" class="wp-section">
      <h3 class="wp-section-title">启停炉记录</h3>
      <div class="wp-panel">
        <div class="wp-panel-body">
          <table class="wp-table">
            <thead>
              <tr>
                <th>炉号</th>
                <th>动作</th>
                <th>操作时间</th>
                <th>月份</th>
                <th class="right">炉膛温度 ℃</th>
                <th class="right">当班处理量 t</th>
                <th class="right">当班发电量 kWh</th>
                <th class="right">运行 h</th>
                <th>操作员</th>
                <th>备注</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="l in logs" :key="l.id">
                <td class="mono strong">{{ l.furnaceCode }}</td>
                <td><span class="wp-tag" :class="actionTag(l.action)">{{ l.actionName }}</span></td>
                <td class="mono">{{ time(l.operateTime) }}</td>
                <td class="mono">{{ l.periodMonth }}</td>
                <td class="right num">{{ num(l.furnaceTempC, 1) }}</td>
                <td class="right num">{{ num(l.processTon, 1) }}</td>
                <td class="right num">{{ num(l.powerKwh, 0) }}</td>
                <td class="right num">{{ num(l.runHours, 1) }}</td>
                <td>{{ l.operator }}</td>
                <td class="dim">{{ l.remark || '--' }}</td>
              </tr>
              <tr v-if="!logs.length"><td colspan="10" class="empty">暂无启停炉记录</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- ===== 锅炉工况弹窗 ===== -->
    <el-dialog v-model="boilerDialog" title="调整余热锅炉蒸汽工况" width="460px">
      <el-form :model="boilerForm" label-width="100px">
        <el-form-item label="蒸汽压力 MPa"><el-input-number v-model="boilerForm.steamPressureMpa" :min="0" :precision="2" :step="0.05" /></el-form-item>
        <el-form-item label="蒸汽温度 ℃"><el-input-number v-model="boilerForm.steamTempC" :min="0" :precision="1" :step="1" /></el-form-item>
        <el-form-item label="蒸发量 t/h"><el-input-number v-model="boilerForm.evaporationTph" :min="0" :precision="2" :step="1" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="boilerForm.status">
            <el-option v-for="s in powerDict.boilerStatuses || []" :key="s.code" :label="s.name" :value="s.code" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="boilerDialog = false">取消</el-button>
        <el-button type="primary" @click="submitBoiler">保存</el-button>
      </template>
    </el-dialog>

    <!-- ===== 发电日结弹窗（填报期末累计表码，不是发电量） ===== -->
    <el-dialog v-model="settleDialog" :title="'发电日结 · ' + settleForm.unitNo" width="560px">
      <el-form :model="settleForm" label-width="130px">
        <el-form-item label="统计日">
          <el-date-picker v-model="settleForm.statDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disableFuture" />
        </el-form-item>
        <el-form-item label="发电表期末表码">
          <el-input-number v-model="settleForm.meterReadingKwh" :min="0" :step="100000" />
          <span class="form-hint">kWh，累计读数；当日增量＝期末表码−上日封账基数</span>
        </el-form-item>
        <el-form-item label="上网表期末表码">
          <el-input-number v-model="settleForm.gridMeterReadingKwh" :min="0" :step="100000" />
          <span class="form-hint">kWh，留空则按 85% 估算上网电量</span>
        </el-form-item>
        <el-form-item label="运行小时 h">
          <el-input-number v-model="settleForm.runningHours" :min="0" :max="24" :precision="1" :step="1" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="settleForm.operator" placeholder="日结员" />
        </el-form-item>
        <div class="limit-tip">
          上日封账基数 <b class="num">{{ num(settleForm.baseReadingKwh, 0) }}</b> kWh，
          预计当日增量 <b class="num">{{ num(settlePreview.generation, 0) }}</b> kWh；
          理论上限 <b class="num">{{ settlePreview.max }}</b> kWh（装机 {{ settleForm.capacityMw }} MW × 运行 {{ settleForm.runningHours || 0 }} h），
          表码回拨/更换请走「表计异常处置」。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="settleDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSettle">日结</el-button>
      </template>
    </el-dialog>

    <!-- ===== 表计回拨/更换处置弹窗 ===== -->
    <el-dialog v-model="meterDialog" :title="'表计异常处置 · ' + meterForm.unitNo" width="600px">
      <el-form :model="meterForm" label-width="140px">
        <el-form-item label="统计日">
          <el-date-picker v-model="meterForm.statDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disableFuture" />
        </el-form-item>
        <el-form-item label="异常类型">
          <el-radio-group v-model="meterForm.eventType">
            <el-radio label="ROLLBACK">表计回拨（读数变小）</el-radio>
            <el-radio label="REPLACE">表计更换</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="本次表码">
          <el-input-number v-model="meterForm.observedReadingKwh" :min="0" :step="100" />
          <span class="form-hint">回拨后读数 / 新表读数 kWh</span>
        </el-form-item>
        <el-form-item label="旧表封账止码">
          <el-input-number v-model="meterForm.oldMeterFinalReadingKwh" :min="0" :step="100000" />
          <span class="form-hint">留空取上日封账基数 {{ num(meterForm.previousBase, 0) }}</span>
        </el-form-item>
        <el-form-item v-if="meterForm.eventType === 'REPLACE'" label="新表起码">
          <el-input-number v-model="meterForm.newMeterStartReadingKwh" :min="0" :step="100" />
          <span class="form-hint">新表起始读数，通常为 0</span>
        </el-form-item>
        <el-form-item label="上网表本次表码">
          <el-input-number v-model="meterForm.gridMeterReadingKwh" :min="0" :step="100000" />
          <span class="form-hint">留空按 85% 估算上网增量</span>
        </el-form-item>
        <el-form-item label="运行小时 h">
          <el-input-number v-model="meterForm.runningHours" :min="0" :max="24" :precision="1" :step="1" />
        </el-form-item>
        <el-form-item label="处置选择">
          <el-radio-group v-model="meterForm.resolution">
            <el-radio label="SETTLED_NEW_BASE">旧表封账并启用新基数（当日＝旧表尾段＋新表首段）</el-radio>
            <el-radio label="HELD_PENDING_CALIBRATION">暂停日结等待校表</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="meterForm.operator" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="meterForm.note" type="textarea" :rows="2" placeholder="封账/挂起理由，留痕可审计" />
        </el-form-item>
        <div class="limit-tip">
          选择会改变后续增量口径：封账后次日起按新表读数做差；挂起期间任何日结都会被拒绝，校表确认新基数后恢复。
        </div>
      </el-form>
      <template #footer>
        <el-button @click="meterDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitMeterEvent">提交并留痕</el-button>
      </template>
    </el-dialog>

    <!-- ===== 校表恢复弹窗 ===== -->
    <el-dialog v-model="releaseDialog" :title="'校表恢复 · ' + releaseForm.unitNo" width="480px">
      <el-form :model="releaseForm" label-width="140px">
        <el-form-item label="确认新基数">
          <el-input-number v-model="releaseForm.releaseReadingKwh" :min="0" :step="100" />
          <span class="form-hint">校表后发电表累计读数 kWh</span>
        </el-form-item>
        <el-form-item label="上网表新基数">
          <el-input-number v-model="releaseForm.gridReleaseReadingKwh" :min="0" :step="100" />
          <span class="form-hint">可选</span>
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="releaseForm.operator" />
        </el-form-item>
        <el-form-item label="校表说明">
          <el-input v-model="releaseForm.note" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="releaseDialog = false">取消</el-button>
        <el-button type="success" :loading="submitting" @click="submitRelease">确认恢复日结</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { furnaceApi, powerApi, disposalApi } from '../api'

const furnaces = ref([])
const typeStats = ref([])
const monthly = ref([])
const logs = ref([])
const boilers = ref([])
const turbines = ref([])
const dailies = ref([])
const powerDict = ref({})
const frozenFurnaces = ref([])
const frozenSet = computed(() => new Set(frozenFurnaces.value || []))

const boilerDialog = ref(false)
const settleDialog = ref(false)
const meterDialog = ref(false)
const releaseDialog = ref(false)
const submitting = ref(false)
const boilerForm = reactive({ boilerNo: '', steamPressureMpa: 4, steamTempC: 400, evaporationTph: 70, status: 'RUNNING' })
const settleForm = reactive({
  unitNo: '', capacityMw: 25, statDate: todayStr(), baseReadingKwh: 0,
  meterReadingKwh: 0, gridMeterReadingKwh: null, runningHours: 24, operator: ''
})
const meterForm = reactive({
  unitNo: '', statDate: todayStr(), eventType: 'ROLLBACK', previousBase: 0,
  observedReadingKwh: 0, oldMeterFinalReadingKwh: null, newMeterStartReadingKwh: 0,
  gridMeterReadingKwh: null, runningHours: 24,
  resolution: 'SETTLED_NEW_BASE', operator: '', note: ''
})
const releaseForm = reactive({
  eventId: null, unitNo: '', releaseReadingKwh: 0, gridReleaseReadingKwh: null, operator: '', note: ''
})

const settlePreview = computed(() => {
  const closing = Number(settleForm.meterReadingKwh) || 0
  const base = Number(settleForm.baseReadingKwh) || 0
  const generation = Math.max(closing - base, 0)
  const max = (Number(settleForm.capacityMw) || 0) * 1000 * (Number(settleForm.runningHours) || 0)
  return { generation, max }
})

function todayStr() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function disableFuture(date) {
  return date.getTime() > Date.now()
}

/**
 * 页面请求凭据：同一次填报即使网络重试 / 重新打开页面再点，也只返回同一份日结。
 * 按 机组+日期 持久化到 localStorage，关闭重开浏览器仍保持幂等；
 * 已成功的日结重发会原样返回（不会产生第二份），次日日期变化自然换新凭据。
 */
function requestKey(prefix, unitNo, statDate) {
  return `power:${prefix}:${unitNo}:${statDate}`
}

function obtainRequestId(prefix, unitNo, statDate) {
  const key = requestKey(prefix, unitNo, statDate)
  let id = localStorage.getItem(key)
  if (!id) {
    id = (window.crypto && window.crypto.randomUUID)
      ? window.crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`
    localStorage.setItem(key, id)
  }
  return id
}

const maxMonthly = computed(() => Math.max(...monthly.value.map((m) => Number(m.processTon) || 0), 1))

function num(v, d = 1) {
  if (v === null || v === undefined) return '--'
  const n = Number(v)
  if (Number.isNaN(n)) return '--'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: d, maximumFractionDigits: d })
}

function pct(rate) {
  return ((rate || 0) * 100).toFixed(1) + '%'
}

function time(v) {
  return v ? String(v).replace('T', ' ').slice(0, 16) : '--'
}

function tempHeight(f) {
  const t = Number(f.furnaceTempC) || 0
  return Math.min(t / 1100, 1) * 100 + '%'
}

function statusDot(f) {
  switch (f.combustionStatus) {
    case 'STABLE': return 'ok'
    case 'STARTING': return 'warn'
    case 'SHUTDOWN': return 'idle'
    default: return 'danger'
  }
}

function statusTag(f) {
  switch (f.combustionStatus) {
    case 'STABLE': return 'ok'
    case 'STARTING': return 'warn'
    case 'SHUTDOWN': return 'primary'
    default: return 'danger'
  }
}

function actionTag(action) {
  if (action === 'START') return 'ok'
  if (action === 'STOP') return 'danger'
  return 'warn'
}

async function load() {
  try {
    const [r1, r2, r3, r4, r5, r6, r7, r8, r9] = await Promise.all([
      furnaceApi.list(),
      furnaceApi.statsByType(),
      furnaceApi.monthly(),
      furnaceApi.logs(),
      powerApi.boilers(),
      powerApi.turbines(),
      powerApi.dict(),
      disposalApi.frozenFurnaces(),
      powerApi.daily()
    ])
    furnaces.value = r1.data || []
    typeStats.value = r2.data || []
    monthly.value = r3.data || []
    logs.value = r4.data || []
    boilers.value = r5.data || []
    turbines.value = r6.data || []
    powerDict.value = r7.data || {}
    frozenFurnaces.value = r8.data || []
    dailies.value = r9.data || []
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function goDisposal(f) {
  window.history.pushState({}, '', '/disposal')
  window.dispatchEvent(new PopStateEvent('popstate'))
}

async function run(f, action) {
  try {
    const res = await furnaceApi.run(f.furnaceCode, { action, operator: '中控值班' })
    ElMessage.success(res.message || '状态已推进')
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function judge(f) {
  try {
    const res = await furnaceApi.judge(f.furnaceCode)
    ElMessage.success(res.data.message)
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openBoiler(b) {
  Object.assign(boilerForm, {
    boilerNo: b.boilerNo,
    steamPressureMpa: Number(b.steamPressureMpa),
    steamTempC: Number(b.steamTempC),
    evaporationTph: Number(b.evaporationTph),
    status: b.status
  })
  boilerDialog.value = true
}

async function submitBoiler() {
  try {
    const res = await powerApi.adjustBoiler(boilerForm.boilerNo, { ...boilerForm })
    ElMessage.success(res.message || '已保存')
    boilerDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

function openSettle(t) {
  Object.assign(settleForm, {
    unitNo: t.unitNo,
    capacityMw: Number(t.capacityMw),
    statDate: todayStr(),
    baseReadingKwh: Number(t.meterReadingKwh || 0),
    meterReadingKwh: Number(t.meterReadingKwh || 0),
    gridMeterReadingKwh: t.gridMeterReadingKwh == null ? null : Number(t.gridMeterReadingKwh),
    runningHours: t.runningHours ? Number(t.runningHours) : 24,
    operator: ''
  })
  settleDialog.value = true
}

async function submitSettle() {
  const statDate = settleForm.statDate || todayStr()
  const payload = {
    statDate,
    meterReadingKwh: settleForm.meterReadingKwh,
    gridMeterReadingKwh: settleForm.gridMeterReadingKwh,
    runningHours: settleForm.runningHours,
    operator: settleForm.operator || '中控值班',
    requestId: obtainRequestId('settle', settleForm.unitNo, statDate)
  }
  submitting.value = true
  try {
    const res = await powerApi.settle(settleForm.unitNo, payload)
    ElMessage.success(`${res.message || '日结完成'}：当日增量 ${num(res.data.generationKwh, 0)} kWh，厂用电率 ${num(res.data.auxiliaryRate, 2)}%`)
    settleDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

function openMeterEvent(t) {
  Object.assign(meterForm, {
    unitNo: t.unitNo,
    statDate: todayStr(),
    eventType: 'ROLLBACK',
    previousBase: Number(t.meterReadingKwh || 0),
    observedReadingKwh: 0,
    oldMeterFinalReadingKwh: null,
    newMeterStartReadingKwh: 0,
    gridMeterReadingKwh: null,
    runningHours: t.runningHours ? Number(t.runningHours) : 24,
    resolution: 'SETTLED_NEW_BASE',
    operator: '',
    note: ''
  })
  meterDialog.value = true
}

async function submitMeterEvent() {
  const statDate = meterForm.statDate || todayStr()
  const payload = {
    statDate,
    eventType: meterForm.eventType,
    observedReadingKwh: meterForm.observedReadingKwh,
    oldMeterFinalReadingKwh: meterForm.oldMeterFinalReadingKwh,
    newMeterStartReadingKwh: meterForm.eventType === 'REPLACE' ? meterForm.newMeterStartReadingKwh : undefined,
    gridMeterReadingKwh: meterForm.gridMeterReadingKwh,
    runningHours: meterForm.runningHours,
    resolution: meterForm.resolution,
    operator: meterForm.operator || '中控值班',
    note: meterForm.note,
    requestId: obtainRequestId('meter', meterForm.unitNo, statDate)
  }
  submitting.value = true
  try {
    const res = await powerApi.meterEvent(meterForm.unitNo, payload)
    ElMessage.success(res.message || '已留痕')
    meterDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

function openRelease(t) {
  Object.assign(releaseForm, {
    eventId: null,
    unitNo: t.unitNo,
    releaseReadingKwh: Number(t.meterReadingKwh || 0),
    gridReleaseReadingKwh: null,
    operator: '',
    note: ''
  })
  releaseDialog.value = true
}

async function submitRelease() {
  // 查该机组挂起事件
  let eventId = releaseForm.eventId
  if (!eventId) {
    const events = await powerApi.meterEvents(releaseForm.unitNo)
    const open = (events.data || []).find((e) => e.eventStatus === 'OPEN')
    if (!open) {
      ElMessage.error('未找到待校表事件，无法恢复')
      return
    }
    eventId = open.id
  }
  submitting.value = true
  try {
    const res = await powerApi.releaseMeter(eventId, {
      releaseReadingKwh: releaseForm.releaseReadingKwh,
      gridReleaseReadingKwh: releaseForm.gridReleaseReadingKwh,
      operator: releaseForm.operator || '校表员',
      note: releaseForm.note
    })
    ElMessage.success(res.message || '已恢复日结')
    releaseDialog.value = false
    await load()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.furnace-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(330px, 1fr));
  gap: 12px;
}

.furnace-card {
  padding: 12px 14px;
  border-radius: 8px;
  background: linear-gradient(180deg, #1b2833 0%, #14202a 100%);
  border: 1px solid var(--wp-border);
  border-top: 3px solid var(--wp-primary-3);
}

.furnace-card.bad {
  border-top-color: var(--wp-danger);
}

.furnace-card.offline {
  opacity: 0.86;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

.fcode {
  font-size: 15px;
  font-weight: 700;
  color: #eaf6fc;
  letter-spacing: 1px;
}

.head-right {
  font-size: 11px;
  color: var(--wp-text-mute);
}

.temp-block {
  display: flex;
  align-items: stretch;
  gap: 14px;
  padding: 8px 0;
  border-top: 1px dashed var(--wp-border);
  border-bottom: 1px dashed var(--wp-border);
}

.temp-read {
  flex: 1;
}

.temp-value {
  font-size: 38px;
  font-weight: 700;
  line-height: 1;
  color: #ffb74d;
}

.temp-value.danger {
  color: var(--wp-danger);
}

.temp-unit {
  margin-top: 2px;
  font-size: 12px;
  color: var(--wp-text-dim);
}

.temp-meta {
  margin-top: 6px;
  font-size: 11px;
  color: var(--wp-text-mute);
}

/* 温度柱 + 850℃ 达标线 */
.temp-col {
  position: relative;
  width: 38px;
  height: 132px;
  background: #0c141a;
  border: 1px solid var(--wp-border-2);
  border-radius: 4px;
  overflow: visible;
}

.temp-fill {
  position: absolute;
  left: 3px;
  right: 3px;
  bottom: 3px;
  border-radius: 3px;
  background: linear-gradient(180deg, #ff8a65, #e65100);
  transition: height 0.5s ease;
}

.temp-fill.danger {
  background: linear-gradient(180deg, #ff5252, #b71c1c);
}

.temp-fill.glow {
  animation: wp-glow 1.1s ease-in-out infinite;
}

.temp-line {
  position: absolute;
  left: -10px;
  right: -46px;
  bottom: 77.27%;
  border-top: 1px dashed var(--wp-warn);
}

.temp-line span {
  position: absolute;
  right: 0;
  top: -14px;
  font-size: 9px;
  color: var(--wp-warn);
  white-space: nowrap;
}

.unqualified {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 8px 0;
  padding: 6px 8px;
  border-radius: 4px;
  background: rgba(239, 83, 80, 0.09);
  border: 1px solid rgba(239, 83, 80, 0.28);
  font-size: 11px;
  color: #ffb4b0;
}

.params {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  margin: 8px 0;
}

.param {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 5px 8px;
  border-radius: 4px;
  background: #101a21;
  border: 1px solid var(--wp-border);
}

.p-label {
  font-size: 11px;
  color: var(--wp-text-dim);
}

.p-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--wp-accent);
}

.card-foot {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 6px 0;
  font-size: 11px;
  color: var(--wp-text-mute);
  border-top: 1px dashed var(--wp-border);
}

.foot-item b {
  color: var(--wp-text);
}

.actions {
  display: flex;
  gap: 6px;
  margin-top: 8px;
}

.frozen-badge {
  animation: wp-glow 1.1s ease-in-out infinite;
}

.freeze-tip {
  margin-top: 8px;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 11px;
  color: #ffb4b0;
  background: rgba(239, 83, 80, 0.09);
  border: 1px solid rgba(239, 83, 80, 0.28);
}

/* 炉型统计 */
.type-grid,
.boiler-grid,
.turbine-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.type-name {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.type-name .code {
  font-size: 11px;
  color: var(--wp-text-mute);
}

.type-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 5px 0;
  border-bottom: 1px dashed #1e2b34;
  font-size: 12px;
  color: var(--wp-text-dim);
}

.type-row b {
  color: var(--wp-text);
  font-size: 13px;
}

.type-row b.danger {
  color: var(--wp-danger);
}

.kv-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
}

.kv {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding: 5px 8px;
  border-radius: 4px;
  background: #101a21;
  border: 1px solid var(--wp-border);
  font-size: 11px;
  color: var(--wp-text-dim);
}

.kv b {
  font-size: 12px;
  color: var(--wp-text);
}

.load-row,
.limit-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  font-size: 11px;
  color: var(--wp-text-dim);
}

.load-row .wp-bar {
  flex: 1;
}

.load-label {
  white-space: nowrap;
}

.limit-row b {
  color: var(--wp-warn);
}

.unit-actions {
  margin-top: 10px;
  text-align: right;
}

.wp-table td.right,
.wp-table th.right {
  text-align: right;
}

.wp-table td.strong {
  color: #eaf6fc;
  font-weight: 600;
}

.wp-table td.dim {
  color: var(--wp-text-mute);
}

.empty {
  text-align: center;
  color: var(--wp-text-mute);
  padding: 20px 0 !important;
}

.limit-tip {
  padding: 8px 10px;
  border-radius: 4px;
  background: #101a21;
  border: 1px solid var(--wp-border);
  font-size: 11px;
  color: var(--wp-text-dim);
}

.limit-tip b {
  color: var(--wp-warn);
}

.meter-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.unit-card.held {
  border: 1px solid rgba(239, 83, 80, 0.45);
}

.kv b.hl {
  color: #ffd54f;
}

.form-hint {
  margin-left: 8px;
  font-size: 11px;
  color: var(--wp-text-mute);
}

.daily-table td.abnormal,
.daily-table tr.abnormal td {
  background: rgba(255, 183, 77, 0.05);
}

.daily-table .basis {
  max-width: 320px;
  white-space: normal;
  font-size: 11px;
}

.wp-tag.warn {
  color: #ffb74d;
  border-color: rgba(255, 183, 77, 0.4);
  background: rgba(255, 183, 77, 0.08);
}
</style>
