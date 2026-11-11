<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Clock, EditPen, Link, Refresh, Trophy, Warning } from '@element-plus/icons-vue'

import {
  fetchLeaderboard,
  type LeaderboardPageData,
  type LeaderboardPeriod
} from '@/api/leaderboard'
import {
  bindExternalAccount,
  deleteManualDeclaration,
  fetchMyExternalAccounts,
  fetchMyManualDeclarations,
  unbindExternalAccount,
  upsertManualDeclaration,
  type ExternalAccount,
  type ManualDeclaration
} from '@/api/external'
import LeaderboardRow from '@/components/leaderboard/LeaderboardRow.vue'
import { verificationLabel } from '@/utils/adminLabels'
import { resolveApiErrorMessage } from '@/utils/apiError'
import { formatDateTime } from '@/utils/format'
import { declarationStatusLabel, declarationStatusType } from '@/utils/leaderboard'

const PAGE_SIZE = 20
const SUGGESTED_PLATFORMS = ['牛客', '洛谷', 'AtCoder']
const CODEFORCES_HANDLE_PATTERN = /^[A-Za-z0-9_-]{3,24}$/

const period = ref<LeaderboardPeriod>('week')
const page = ref(1)
const ranking = ref<LeaderboardPageData | null>(null)
const loading = ref(true)
const errorMessage = ref('')

const accounts = ref<ExternalAccount[]>([])
const accountsLoading = ref(false)
const accountsError = ref('')

const declarations = ref<ManualDeclaration[]>([])
const declarationsLoading = ref(false)
const declarationError = ref('')
const declarationWeekStart = ref('')

const manualDialogVisible = ref(false)
const manualSubmitting = ref(false)
const manualFormRef = ref<FormInstance>()
const manualForm = reactive({
  platform: '牛客',
  account: '',
  profileUrl: '',
  count: 0
})
const manualRules: FormRules = {
  platform: [
    { required: true, message: '请选择或输入平台', trigger: 'change' },
    { max: 32, message: '平台名称不能超过 32 个字符', trigger: 'blur' }
  ],
  account: [
    { required: true, message: '请输入平台账号', trigger: 'blur' },
    { max: 128, message: '平台账号不能超过 128 个字符', trigger: 'blur' }
  ],
  count: [{ required: true, message: '请输入本周过题数', trigger: 'change' }]
}

const cfDialogVisible = ref(false)
const cfSubmitting = ref(false)
const cfForm = reactive({
  handle: '',
  profileUrl: ''
})

const isWeekly = computed(() => period.value === 'week')
const cfAccount = computed(
  () => accounts.value.find((account) => account.platform === 'codeforces') ?? null
)
const me = computed(() => ranking.value?.me ?? null)
const total = computed(() => ranking.value?.total ?? 0)
const meInPage = computed(() => ranking.value?.items.some((item) => item.isMe) ?? false)

const infoText = computed(() =>
  isWeekly.value
    ? '周榜统计北京时间自然周（周一 00:00 至下周一 00:00）内产生的成绩：站内仅计 OJ 首次 AC，CF 取当前绑定账号的本周同步值，其他平台取本周审核通过的手动申报。平台内去重，不做跨平台去重。'
    : '总榜统计平台上线以来的全部历史成绩：站内取历史首次 AC，CF 取绑定账号的历史累计过题数，手动申报累计全部审核通过记录。总榜只读，不提供申报入口。'
)

const boardTitle = computed(() => (isWeekly.value ? '周榜排名' : '总榜排名'))
const boardDescription = computed(() =>
  isWeekly.value
    ? '总分相同按站内首次 AC 数降序；仍相同则并列。分页展示全部上榜选手。'
    : '总分相同按站内历史首次 AC 数降序；仍相同则并列。总榜只读。'
)

function displayTime(value: string | null): string {
  return value ? formatDateTime(value) : '—'
}

function isValidOptionalUrl(value: string): boolean {
  const trimmed = value.trim()
  if (!trimmed) {
    return true
  }
  if (trimmed.length > 512) {
    return false
  }
  try {
    const url = new URL(trimmed)
    return url.protocol === 'http:' || url.protocol === 'https:'
  } catch {
    return false
  }
}

async function loadRanking() {
  loading.value = true
  try {
    ranking.value = await fetchLeaderboard({
      period: period.value,
      page: page.value,
      pageSize: PAGE_SIZE
    })
    errorMessage.value = ''
  } catch (error) {
    const message = resolveApiErrorMessage(error, '榜单加载失败，请稍后重试')
    if (ranking.value) {
      ElMessage.error(message)
    } else {
      errorMessage.value = message
    }
  } finally {
    loading.value = false
  }
}

async function loadAccounts() {
  accountsLoading.value = true
  accountsError.value = ''
  try {
    accounts.value = (await fetchMyExternalAccounts()).items
  } catch (error) {
    accounts.value = []
    accountsError.value = resolveApiErrorMessage(error, '外部账号加载失败')
  } finally {
    accountsLoading.value = false
  }
}

async function loadDeclarations() {
  declarationsLoading.value = true
  declarationError.value = ''
  try {
    const result = await fetchMyManualDeclarations({ page: 1, pageSize: 100 })
    declarations.value = result.items
    declarationWeekStart.value = result.weekStartDate
  } catch (error) {
    declarations.value = []
    declarationError.value = resolveApiErrorMessage(error, '本周申报记录加载失败')
  } finally {
    declarationsLoading.value = false
  }
}

function switchPeriod(next: LeaderboardPeriod) {
  if (period.value === next) {
    return
  }
  period.value = next
  page.value = 1
  ranking.value = null
  errorMessage.value = ''
  void loadRanking()
}

function changePage(nextPage: number) {
  page.value = nextPage
  void loadRanking()
}

function resetManualForm() {
  manualForm.platform = '牛客'
  manualForm.account = ''
  manualForm.profileUrl = ''
  manualForm.count = 0
  manualFormRef.value?.clearValidate()
}

function fillManualForm(declaration: ManualDeclaration) {
  manualForm.platform = declaration.platform
  manualForm.account = declaration.account
  manualForm.profileUrl = declaration.profileUrl ?? ''
  manualForm.count = declaration.count
  manualFormRef.value?.clearValidate()
}

function openManualDialog(declaration?: ManualDeclaration) {
  resetManualForm()
  const source = declaration ?? declarations.value[0]
  if (source) {
    fillManualForm(source)
  }
  manualDialogVisible.value = true
  void loadDeclarations()
}

function editDeclaration(declaration: ManualDeclaration) {
  fillManualForm(declaration)
}

async function submitManual() {
  if (manualSubmitting.value) {
    return
  }
  const form = manualFormRef.value
  if (!form) {
    return
  }
  const valid = await form.validate().catch(() => false)
  if (!valid) {
    return
  }
  if (!isValidOptionalUrl(manualForm.profileUrl)) {
    ElMessage.warning('主页地址需为合法的 http/https URL，且不超过 512 个字符')
    return
  }

  manualSubmitting.value = true
  try {
    await upsertManualDeclaration({
      platform: manualForm.platform.trim(),
      account: manualForm.account.trim(),
      profileUrl: manualForm.profileUrl.trim() || null,
      count: manualForm.count
    })
    ElMessage.success('申报已保存，等待超级管理员审核通过后计入榜单')
    manualDialogVisible.value = false
    await Promise.all([loadDeclarations(), loadRanking()])
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '保存申报失败，请稍后重试'))
  } finally {
    manualSubmitting.value = false
  }
}

async function handleDeleteDeclaration(declaration: ManualDeclaration) {
  try {
    await ElMessageBox.confirm(
      `确认删除「${declaration.platform}」本周的申报记录？删除后立即不计入榜单。`,
      '删除手动申报',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  try {
    await deleteManualDeclaration(declaration.id)
    ElMessage.success('申报已删除')
    if (manualForm.platform === declaration.platform) {
      resetManualForm()
    }
    await Promise.all([loadDeclarations(), loadRanking()])
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '删除申报失败，请稍后重试'))
  }
}

function openCfDialog() {
  cfForm.handle = ''
  cfForm.profileUrl = ''
  accountsError.value = ''
  cfDialogVisible.value = true
  void loadAccounts()
}

async function submitBind() {
  if (cfSubmitting.value) {
    return
  }
  const handle = cfForm.handle.trim()
  if (!CODEFORCES_HANDLE_PATTERN.test(handle)) {
    ElMessage.warning('Handle 需为 3–24 位字母、数字、下划线或连字符')
    return
  }
  if (!isValidOptionalUrl(cfForm.profileUrl)) {
    ElMessage.warning('主页地址需为合法的 http/https URL，且不超过 512 个字符')
    return
  }

  cfSubmitting.value = true
  try {
    await bindExternalAccount({
      platform: 'codeforces',
      handle,
      profileUrl: cfForm.profileUrl.trim() || null
    })
    ElMessage.success('CF 账号已绑定，真实过题数据已计入榜单')
    await Promise.all([loadAccounts(), loadRanking()])
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '绑定 CF 账号失败，请稍后重试'))
  } finally {
    cfSubmitting.value = false
  }
}

async function handleUnbindAccount() {
  const account = cfAccount.value
  if (!account) {
    return
  }
  try {
    await ElMessageBox.confirm(
      '解绑后该 CF 账号立即不再计入榜单，如需重新计分请再次绑定。确认解绑吗？',
      '解绑 Codeforces',
      {
        confirmButtonText: '解绑',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
  } catch {
    return
  }

  cfSubmitting.value = true
  try {
    await unbindExternalAccount(account.id)
    ElMessage.success('CF 账号已解绑')
    await Promise.all([loadAccounts(), loadRanking()])
  } catch (error) {
    ElMessage.error(resolveApiErrorMessage(error, '解绑 CF 账号失败，请稍后重试'))
  } finally {
    cfSubmitting.value = false
  }
}

onMounted(() => {
  void loadRanking()
  void loadAccounts()
  void loadDeclarations()
})
</script>

<template>
  <div class="learning-page leaderboard-page">
    <section class="leaderboard-hero">
      <div class="leaderboard-hero__copy">
        <span class="learning-eyebrow">CROSS-PLATFORM RANKING</span>
        <h1>每一道 AC，<br /><span>都在这里留下刻度。</span></h1>
        <p>
          站内首次 AC、Codeforces 过题与审核通过的手动申报合成全站排名。
          周榜记录当下冲刺，总榜沉淀长期积累。
        </p>
        <div class="leaderboard-hero__actions">
          <el-button
            v-if="isWeekly"
            type="primary"
            :icon="EditPen"
            @click="openManualDialog()"
          >
            申报本周成绩
          </el-button>
          <el-button :icon="Link" @click="openCfDialog">
            {{ cfAccount ? '管理 CF 绑定' : '绑定 CF 账号' }}
          </el-button>
        </div>
      </div>

      <div class="leaderboard-hero__panel">
        <span class="leaderboard-hero__panel-label">
          {{ isWeekly ? '本周参与统计' : '历史累计上榜' }}
        </span>
        <strong class="leaderboard-hero__count">{{ total }}</strong>
        <span class="leaderboard-hero__count-label">位选手</span>
        <div class="leaderboard-hero__divider"></div>
        <div class="leaderboard-hero__meta">
          <el-icon><Trophy /></el-icon>
          {{ isWeekly ? '北京时间自然周' : '全部历史累计' }}
        </div>
        <div class="leaderboard-hero__meta">
          <el-icon><Clock /></el-icon>
          更新于 {{ ranking ? formatDateTime(ranking.generatedAt) : '—' }}
        </div>
      </div>
    </section>

    <div class="lb-page-tabs" role="tablist" aria-label="榜单周期">
      <button
        type="button"
        class="lb-tab"
        :class="{ active: isWeekly }"
        role="tab"
        :aria-selected="isWeekly"
        @click="switchPeriod('week')"
      >
        周榜
      </button>
      <button
        type="button"
        class="lb-tab"
        :class="{ active: !isWeekly }"
        role="tab"
        :aria-selected="!isWeekly"
        @click="switchPeriod('all')"
      >
        总榜
      </button>
    </div>

    <section class="leaderboard-info">
      <span class="leaderboard-info__icon">i</span>
      <div class="leaderboard-info__copy">
        <strong>统计口径</strong>
        <p>{{ infoText }}</p>
      </div>
      <span class="leaderboard-info__meta">
        {{ ranking?.weekStartDate ? `统计周 ${ranking.weekStartDate} 起` : '全部历史累计' }}
        <br />
        榜单实时生成，禁用用户不参与排名
      </span>
    </section>

    <section v-if="ranking && me" class="my-rank-card">
      <div class="my-rank-card__head">
        <span class="my-rank-card__badge">我的排名</span>
        <strong class="my-rank-card__rank">#{{ me.rank }}</strong>
        <span class="my-rank-card__sub">共 {{ total }} 位选手参与统计</span>
      </div>
      <div class="my-rank-card__stats">
        <div>
          <span>{{ isWeekly ? '站内过题' : '站内累计' }}</span>
          <strong>{{ me.internalSolved }}</strong>
          <em>首次 AC</em>
        </div>
        <div>
          <span>{{ isWeekly ? 'CF 过题' : 'CF 累计' }}</span>
          <strong>{{ me.cfSolved }}</strong>
          <em>{{ isWeekly ? '本周同步' : '历史同步' }}</em>
        </div>
        <div>
          <span>{{ isWeekly ? '手动申报' : '手动累计' }}</span>
          <strong>{{ me.manualSolved }}</strong>
          <em>审核通过</em>
        </div>
        <div class="is-total">
          <span>总分</span>
          <strong>{{ me.totalScore }}</strong>
          <em>三项之和</em>
        </div>
      </div>
      <div class="my-rank-card__actions">
        <el-button
          v-if="isWeekly"
          type="primary"
          size="small"
          :icon="EditPen"
          @click="openManualDialog()"
        >
          申报本周成绩
        </el-button>
        <el-button size="small" :icon="Link" @click="openCfDialog">
          {{ cfAccount ? '管理 CF 绑定' : '绑定 CF' }}
        </el-button>
      </div>
    </section>

    <section v-else-if="ranking" class="my-rank-card my-rank-card--empty">
      <div class="my-rank-card__head">
        <span class="my-rank-card__badge">我的排名</span>
        <strong class="my-rank-card__empty-title">暂未上榜</strong>
      </div>
      <p class="my-rank-card__empty-text">
        完成任意一道 OJ 题目的首次 AC，或绑定 CF 账号后即可进入榜单；手动申报需审核通过后计分。
      </p>
      <div class="my-rank-card__actions">
        <el-button
          v-if="isWeekly"
          type="primary"
          size="small"
          :icon="EditPen"
          @click="openManualDialog()"
        >
          申报本周成绩
        </el-button>
        <el-button size="small" :icon="Link" @click="openCfDialog">
          {{ cfAccount ? '管理 CF 绑定' : '绑定 CF' }}
        </el-button>
      </div>
    </section>

    <section class="leaderboard-board">
      <div class="leaderboard-board__head">
        <div>
          <h2>{{ boardTitle }}</h2>
          <p>{{ boardDescription }}</p>
        </div>
        <div class="leaderboard-board__actions">
          <span class="learning-count">共 {{ total }} 位选手</span>
          <el-button :icon="Refresh" :loading="loading" @click="loadRanking">刷新</el-button>
        </div>
      </div>

      <div v-if="loading && !ranking" class="leaderboard-skeleton" aria-busy="true">
        <el-skeleton :rows="8" animated />
      </div>

      <div v-else-if="errorMessage" class="learning-state">
        <span class="learning-state__icon learning-state__icon--danger">!</span>
        <h3>榜单暂时没有加载出来</h3>
        <p>{{ errorMessage }}</p>
        <el-button :icon="Refresh" @click="loadRanking">重新加载</el-button>
      </div>

      <div v-else-if="!ranking || ranking.items.length === 0" class="learning-state">
        <span class="learning-state__icon">◌</span>
        <h3>{{ isWeekly ? '本周还没有选手上榜' : '总榜还没有选手上榜' }}</h3>
        <p>完成站内首次 AC、绑定 CF 账号或提交手动申报，都会成为榜单的一部分。</p>
      </div>

      <template v-else>
        <div class="leaderboard-card">
          <div class="lb-table-wrap">
            <table class="lb-table">
              <thead>
                <tr>
                  <th class="lb-col-rank">排名</th>
                  <th>选手</th>
                  <th class="lb-col-num">{{ isWeekly ? '站内' : '站内累计' }}</th>
                  <th class="lb-col-num">{{ isWeekly ? 'CF' : 'CF 累计' }}</th>
                  <th class="lb-col-num">{{ isWeekly ? '手动' : '手动累计' }}</th>
                  <th class="lb-col-num">总分</th>
                  <th>数据来源</th>
                </tr>
              </thead>
              <tbody>
                <LeaderboardRow
                  v-for="item in ranking.items"
                  :key="item.user.id"
                  :item="item"
                />
                <template v-if="!meInPage && ranking.me">
                  <tr class="lb-separator">
                    <td colspan="7">我的排名</td>
                  </tr>
                  <LeaderboardRow :item="ranking.me" />
                </template>
              </tbody>
            </table>
          </div>

          <div v-if="ranking.total > PAGE_SIZE" class="learning-pagination">
            <el-pagination
              background
              layout="prev, pager, next"
              :total="ranking.total"
              :page-size="PAGE_SIZE"
              :current-page="page"
              @current-change="changePage"
            />
          </div>
        </div>

        <div class="lb-legend">
          <span><i class="lb-dot lb-dot--internal"></i>站内首次 AC</span>
          <span><i class="lb-dot lb-dot--cf"></i>CF API 同步</span>
          <span><i class="lb-dot lb-dot--manual"></i>手动申报（审核通过后计分）</span>
          <span class="lb-legend__note">总分相同按站内过题数降序；仍相同则并列</span>
        </div>
      </template>
    </section>

    <el-dialog
      v-model="manualDialogVisible"
      class="leaderboard-dialog"
      width="min(580px, 94vw)"
      title="手动申报本周过题数"
    >
      <div class="leaderboard-dialog__hint">
        <el-icon><Warning /></el-icon>
        <p>
          申报范围为北京时间当前自然周。同一平台再次提交会覆盖原记录并重置审核状态；
          审核通过前不会计入榜单。
        </p>
      </div>

      <el-form
        ref="manualFormRef"
        :model="manualForm"
        :rules="manualRules"
        label-position="top"
        @submit.prevent
      >
        <div class="leaderboard-form-grid">
          <el-form-item label="平台" prop="platform">
            <el-select
              v-model="manualForm.platform"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入平台"
            >
              <el-option
                v-for="platformOption in SUGGESTED_PLATFORMS"
                :key="platformOption"
                :label="platformOption"
                :value="platformOption"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="本周过题数" prop="count">
            <el-input-number
              v-model="manualForm.count"
              :min="0"
              :max="100000"
              :step="1"
              step-strictly
              controls-position="right"
            />
          </el-form-item>
        </div>
        <el-form-item label="平台账号" prop="account">
          <el-input v-model="manualForm.account" maxlength="128" placeholder="例如 demo_nk" />
        </el-form-item>
        <el-form-item label="主页地址（选填）">
          <el-input
            v-model="manualForm.profileUrl"
            maxlength="512"
            placeholder="https://example.com/profile/demo_nk"
          />
        </el-form-item>
      </el-form>

      <div class="leaderboard-declarations">
        <div class="leaderboard-declarations__head">
          <strong>本周已提交</strong>
          <span v-if="declarationWeekStart">统计周 {{ declarationWeekStart }}</span>
        </div>
        <p v-if="declarationError" class="leaderboard-inline-error">{{ declarationError }}</p>
        <p v-else-if="declarationsLoading" class="lb-muted">正在加载申报记录…</p>
        <p v-else-if="declarations.length === 0" class="lb-muted">本周还没有申报记录。</p>
        <div v-else class="leaderboard-declarations__list">
          <div
            v-for="declaration in declarations"
            :key="declaration.id"
            class="declaration-item"
          >
            <div class="declaration-item__main">
              <strong>{{ declaration.platform }}</strong>
              <span>{{ declaration.account }}</span>
              <small>{{ declaration.count }} 题 · 更新于 {{ displayTime(declaration.updatedAt) }}</small>
            </div>
            <el-tag
              :type="declarationStatusType(declaration.verificationStatus)"
              effect="plain"
              size="small"
            >
              {{ declarationStatusLabel(declaration.verificationStatus) }}
            </el-tag>
            <div class="declaration-item__actions">
              <el-button link type="primary" @click="editDeclaration(declaration)">修改</el-button>
              <el-button link type="danger" @click="handleDeleteDeclaration(declaration)">
                删除
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="manualDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualSubmitting" @click="submitManual">
          保存申报
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="cfDialogVisible"
      class="leaderboard-dialog"
      width="min(540px, 94vw)"
      :title="cfAccount ? '管理 Codeforces 绑定' : '绑定 Codeforces 账号'"
    >
      <div v-if="accountsLoading" class="leaderboard-dialog__loading">
        <el-skeleton :rows="3" animated />
      </div>

      <template v-else-if="cfAccount">
        <div class="cf-account-card">
          <div class="cf-account-card__mark">CF</div>
          <div class="cf-account-card__text">
            <strong>{{ cfAccount.handle }}</strong>
            <span>
              {{ verificationLabel(cfAccount.verificationStatus) }} ·
              数据来源 {{ cfAccount.dataSource === 'api' ? 'API' : '模拟' }}
            </span>
            <small>
              本周 {{ cfAccount.weeklyCount }} 题 · 历史累计 {{ cfAccount.allTimeCount }} 题
            </small>
            <small>最近同步 {{ displayTime(cfAccount.lastSyncAt) }}</small>
          </div>
          <el-tag type="success" effect="plain">已绑定</el-tag>
        </div>
        <p class="leaderboard-dialog__note">
          CF 数据由平台统一刷新，选手端仅展示最近同步时间。如需更换 Handle，请先解绑再重新绑定；
          解绑后该账号立即不再计入榜单。
        </p>
        <div class="leaderboard-dialog__actions">
          <el-button type="danger" plain :loading="cfSubmitting" @click="handleUnbindAccount">
            解绑账号
          </el-button>
        </div>
      </template>

      <template v-else>
        <p v-if="accountsError" class="leaderboard-inline-error">{{ accountsError }}</p>
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="Codeforces Handle" required>
            <el-input
              v-model="cfForm.handle"
              maxlength="64"
              placeholder="例如 tourist"
              @keyup.enter="submitBind"
            />
          </el-form-item>
          <el-form-item label="主页地址（选填）">
            <el-input
              v-model="cfForm.profileUrl"
              maxlength="512"
              placeholder="留空自动生成 Codeforces 主页"
            />
          </el-form-item>
        </el-form>
        <p class="leaderboard-dialog__note">
          绑定时通过 Codeforces 公共 API 校验 Handle，并读取当前周与历史累计过题数；
          提交记录较多时可能需要几秒。同一 Handle 只能被一个有效绑定占用。
        </p>
        <div class="leaderboard-dialog__actions">
          <el-button @click="cfDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="cfSubmitting" @click="submitBind">
            保存绑定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
