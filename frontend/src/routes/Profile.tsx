import { appsBase, statsBase } from '../api/client'

export default function Profile() {
  return (
    <>
      <h2>Profile</h2>
      <form className="grid">
        <label>
          Timezone
          <input defaultValue="America/Phoenix" />
        </label>
        <label>
          Default Range
          <select defaultValue="30d">
            <option value="7d">Last 7 days</option>
            <option value="30d">Last 30 days</option>
          </select>
        </label>
        <button type="button" className="contrast">Save</button>
      </form>

      <article style={{ marginTop: '1rem' }}>
        <header>API Configuration</header>
        <p><strong>Applications API:</strong> {appsBase}</p>
        <p><strong>Stats API:</strong> {statsBase}</p>
      </article>
    </>
  )
}
