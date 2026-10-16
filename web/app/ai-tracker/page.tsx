const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL!
const supabaseAnonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY!

interface AiMetric {
  id: string
  created_at: string
  feature: string
  action: string | null
  provider: string
  model: string
  mock: boolean
  latency_ms: number | null
  input_tokens: number | null
  output_tokens: number | null
  total_tokens: number | null
  cost_usd: number | null
  success: boolean
  error_message: string | null
  error_code: string | null
  user_id: string | null
  organization_id: string | null
  report_id: string | null
}

async function fetchMetrics(): Promise<AiMetric[]> {
  const res = await fetch(
    `${supabaseUrl}/rest/v1/ai_metrics?select=*&order=created_at.desc`,
    {
      headers: {
        apikey: supabaseAnonKey,
        Authorization: `Bearer ${supabaseAnonKey}`,
      },
    }
  )
  if (!res.ok) return []
  return res.json()
}

export default async function AiTrackerPage() {
  const metrics = await fetchMetrics()

  const totalCalls = metrics.length
  const successfulCalls = metrics.filter((m) => m.success).length
  const failedCalls = totalCalls - successfulCalls
  const successRate = totalCalls > 0 ? ((successfulCalls / totalCalls) * 100).toFixed(1) : "0"

  const latencies = metrics.filter((m) => m.latency_ms != null).map((m) => m.latency_ms!)
  const avgLatency =
    latencies.length > 0
      ? Math.round(latencies.reduce((a, b) => a + b, 0) / latencies.length)
      : 0

  const totalCost = metrics.reduce((sum, m) => sum + (m.cost_usd || 0), 0)

  const mockCalls = metrics.filter((m) => m.mock).length
  const realCalls = totalCalls - mockCalls

  // Provider breakdown
  const providerMap = metrics.reduce(
    (acc, m) => {
      if (!acc[m.provider]) acc[m.provider] = { count: 0, mock: 0, success: 0 }
      acc[m.provider].count++
      if (m.mock) acc[m.provider].mock++
      if (m.success) acc[m.provider].success++
      return acc
    },
    {} as Record<string, { count: number; mock: number; success: number }>
  )

  // Feature breakdown
  const featureMap = metrics.reduce(
    (acc, m) => {
      if (!acc[m.feature]) acc[m.feature] = { count: 0, success: 0, totalLatency: 0, latencyCount: 0 }
      acc[m.feature].count++
      if (m.success) acc[m.feature].success++
      if (m.latency_ms != null) {
        acc[m.feature].totalLatency += m.latency_ms
        acc[m.feature].latencyCount++
      }
      return acc
    },
    {} as Record<string, { count: number; success: number; totalLatency: number; latencyCount: number }>
  )

  const recentMetrics = metrics.slice(0, 50)

  return (
    <>
      <section className="bg-linen">
        <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 md:py-16">
          <p className="text-sm font-bold uppercase tracking-widest text-terracotta">
            AI Dashboard
          </p>
          <h1 className="mt-3 text-4xl font-extrabold tracking-tight text-ink sm:text-5xl">
            AI Tracker
          </h1>
          <p className="mt-4 text-lg leading-8 text-ink-variant">
            Monitor AI feature usage, performance, and costs across all providers.
          </p>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-8 sm:px-6">
        {/* Key Metrics */}
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard label="Total AI Calls" value={String(totalCalls)} sub={`${realCalls} real, ${mockCalls} mock`} />
          <StatCard label="Success Rate" value={`${successRate}%`} sub={`${successfulCalls} ok, ${failedCalls} failed`} />
          <StatCard label="Avg Latency" value={`${avgLatency}ms`} sub="Across all providers" />
          <StatCard label="Total Cost" value={`$${totalCost.toFixed(4)}`} sub="Estimated USD" />
        </div>

        {/* Provider & Feature tables */}
        <div className="mt-10 grid gap-6 md:grid-cols-2">
          <div className="rounded-2xl border border-clay-deep bg-white p-6">
            <h2 className="text-xl font-extrabold tracking-tight text-ink">Provider Breakdown</h2>
            <p className="mt-1 text-sm text-ink-variant">AI calls by provider</p>
            <table className="mt-4 w-full text-sm">
              <thead>
                <tr className="border-b border-clay-deep text-left text-ink-variant">
                  <th className="pb-2 font-semibold">Provider</th>
                  <th className="pb-2 text-right font-semibold">Calls</th>
                  <th className="pb-2 text-right font-semibold">Mock</th>
                  <th className="pb-2 text-right font-semibold">Success</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(providerMap).map(([provider, stats]) => (
                  <tr key={provider} className="border-b border-clay text-ink">
                    <td className="py-2 font-medium">{provider}</td>
                    <td className="py-2 text-right">{stats.count}</td>
                    <td className="py-2 text-right">{stats.mock}</td>
                    <td className="py-2 text-right">
                      {stats.count > 0 ? Math.round((stats.success / stats.count) * 100) : 0}%
                    </td>
                  </tr>
                ))}
                {Object.keys(providerMap).length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-6 text-center text-ink-variant">
                      No AI calls recorded yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <div className="rounded-2xl border border-clay-deep bg-white p-6">
            <h2 className="text-xl font-extrabold tracking-tight text-ink">Feature Usage</h2>
            <p className="mt-1 text-sm text-ink-variant">AI calls by feature</p>
            <table className="mt-4 w-full text-sm">
              <thead>
                <tr className="border-b border-clay-deep text-left text-ink-variant">
                  <th className="pb-2 font-semibold">Feature</th>
                  <th className="pb-2 text-right font-semibold">Calls</th>
                  <th className="pb-2 text-right font-semibold">Avg Latency</th>
                  <th className="pb-2 text-right font-semibold">Success</th>
                </tr>
              </thead>
              <tbody>
                {Object.entries(featureMap).map(([feature, stats]) => {
                  const avg = stats.latencyCount > 0 ? Math.round(stats.totalLatency / stats.latencyCount) : 0
                  return (
                    <tr key={feature} className="border-b border-clay text-ink">
                      <td className="py-2 font-medium">{feature}</td>
                      <td className="py-2 text-right">{stats.count}</td>
                      <td className="py-2 text-right">{avg}ms</td>
                      <td className="py-2 text-right">
                        {stats.count > 0 ? Math.round((stats.success / stats.count) * 100) : 0}%
                      </td>
                    </tr>
                  )
                })}
                {Object.keys(featureMap).length === 0 && (
                  <tr>
                    <td colSpan={4} className="py-6 text-center text-ink-variant">
                      No AI calls recorded yet
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Recent Activity */}
        <div className="mt-10 rounded-2xl border border-clay-deep bg-white p-6">
          <h2 className="text-xl font-extrabold tracking-tight text-ink">Recent Activity</h2>
          <p className="mt-1 text-sm text-ink-variant">Last 50 AI calls</p>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-clay-deep text-left text-ink-variant">
                  <th className="pb-2 font-semibold">Time</th>
                  <th className="pb-2 font-semibold">Feature</th>
                  <th className="pb-2 font-semibold">Provider</th>
                  <th className="pb-2 font-semibold">Model</th>
                  <th className="pb-2 text-right font-semibold">Latency</th>
                  <th className="pb-2 text-right font-semibold">Status</th>
                </tr>
              </thead>
              <tbody>
                {recentMetrics.map((m) => (
                  <tr key={m.id} className="border-b border-clay text-ink">
                    <td className="py-2 font-mono text-xs text-ink-variant">
                      {new Date(m.created_at).toLocaleString()}
                    </td>
                    <td className="py-2 font-medium">{m.feature}</td>
                    <td className="py-2">
                      <span
                        className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          m.mock ? "bg-clay text-ink-variant" : "bg-emerald-mist text-emerald-deep"
                        }`}
                      >
                        {m.provider}
                      </span>
                    </td>
                    <td className="py-2 font-mono text-xs text-ink-variant">{m.model}</td>
                    <td className="py-2 text-right">{m.latency_ms ? `${m.latency_ms}ms` : "-"}</td>
                    <td className="py-2 text-right">
                      <span
                        className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                          m.success ? "bg-emerald-mist text-emerald-deep" : "bg-terracotta-mist text-terracotta-deep"
                        }`}
                      >
                        {m.success ? "OK" : "FAIL"}
                      </span>
                    </td>
                  </tr>
                ))}
                {recentMetrics.length === 0 && (
                  <tr>
                    <td colSpan={6} className="py-8 text-center text-ink-variant">
                      No AI metrics recorded yet. Metrics will appear once the civic-ai edge function processes requests.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </>
  )
}

function StatCard({ label, value, sub }: { label: string; value: string; sub: string }) {
  return (
    <div className="rounded-2xl border border-clay-deep bg-white px-5 py-4">
      <p className="text-sm font-semibold text-ink-variant">{label}</p>
      <p className="mt-1 text-3xl font-extrabold tracking-tight text-ink">{value}</p>
      <p className="mt-1 text-xs text-ink-variant">{sub}</p>
    </div>
  )
}
