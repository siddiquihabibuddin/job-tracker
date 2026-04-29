import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQueryClient } from '@tanstack/react-query'
import { checkout } from '../api/billing'
import { useAuth } from '../auth/AuthContext'
import { usePremium } from '../auth/usePremium'

// Format raw digit string into "XXXX XXXX XXXX XXXX"
function formatCardNumber(raw: string): string {
  const digits = raw.replace(/\D/g, '').slice(0, 16)
  return digits.replace(/(.{4})/g, '$1 ').trim()
}

const FEATURE_LIST = [
  'AI smart-fill on new applications',
  'AI insights on your dashboard',
  'CSV spreadsheet import',
  'Folder bulk import (multi-CSV)',
  'Bulk delete applications',
]

function CheckIcon() {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      width="14"
      height="14"
      viewBox="0 0 24 24"
      fill="none"
      stroke="var(--pico-primary)"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      style={{ flexShrink: 0, marginTop: '1px' }}
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  )
}

interface FormErrors {
  nameOnCard?: string
  cardNumber?: string
  expMonth?: string
  expYear?: string
  cvc?: string
}

export default function Upgrade() {
  const nav = useNavigate()
  const { user, setUser } = useAuth()
  const isPremium = usePremium()
  const queryClient = useQueryClient()

  const [nameOnCard, setNameOnCard] = useState('')
  const [cardNumber, setCardNumber] = useState('')
  const [expMonth, setExpMonth] = useState('')
  const [expYear, setExpYear] = useState('')
  const [cvc, setCvc] = useState('')

  const [errors, setErrors] = useState<FormErrors>({})
  const [submitting, setSubmitting] = useState(false)
  const [apiError, setApiError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  function handleCardNumberChange(e: React.ChangeEvent<HTMLInputElement>) {
    setCardNumber(formatCardNumber(e.target.value))
  }

  function validate(): FormErrors {
    const errs: FormErrors = {}
    if (!nameOnCard.trim()) errs.nameOnCard = 'Cardholder name is required.'
    const rawDigits = cardNumber.replace(/\s/g, '')
    if (rawDigits.length < 13 || rawDigits.length > 19) errs.cardNumber = 'Enter a valid card number (13–19 digits).'
    const month = Number(expMonth)
    if (!expMonth || isNaN(month) || month < 1 || month > 12) errs.expMonth = 'Enter a month between 1 and 12.'
    const year = Number(expYear)
    const currentYear = new Date().getFullYear()
    if (!expYear || isNaN(year) || year < currentYear || year > currentYear + 20) errs.expYear = `Enter a year between ${currentYear} and ${currentYear + 20}.`
    if (!cvc || !/^\d{3,4}$/.test(cvc)) errs.cvc = 'Enter a 3 or 4 digit CVC.'
    return errs
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setApiError(null)
    const errs = validate()
    if (Object.keys(errs).length > 0) {
      setErrors(errs)
      return
    }
    setErrors({})
    setSubmitting(true)
    try {
      const resp = await checkout({
        cardNumber: cardNumber.replace(/\s/g, ''),
        expMonth: Number(expMonth),
        expYear: Number(expYear),
        cvc,
        nameOnCard: nameOnCard.trim(),
      })
      sessionStorage.setItem('jt_token', resp.token)
      if (user) {
        setUser({ ...user, tier: 'PREMIUM' })
      }
      await queryClient.invalidateQueries()
      setSuccess(true)
      setTimeout(() => nav('/dashboard'), 1200)
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Payment failed — please try again.'
      setApiError(msg)
    } finally {
      setSubmitting(false)
    }
  }

  if (isPremium && !success) {
    return (
      <div style={{ maxWidth: 540, margin: '4rem auto', textAlign: 'center' }}>
        <article style={{ padding: '2rem' }}>
          <div style={{ fontSize: '1.5rem', marginBottom: '0.5rem' }}>You are already on Pro</div>
          <p style={{ color: 'var(--pico-muted-color)', marginBottom: '1.25rem' }}>
            Your account has full access to all Pro features.
          </p>
          <button className="contrast" onClick={() => nav('/dashboard')} style={{ width: 'auto', padding: '8px 24px' }}>
            Back to Dashboard
          </button>
        </article>
      </div>
    )
  }

  if (success) {
    return (
      <div style={{ maxWidth: 480, margin: '4rem auto', textAlign: 'center' }}>
        <article style={{ padding: '2rem', borderLeft: '3px solid var(--pico-primary)' }}>
          <div style={{ fontSize: '1.4rem', fontWeight: 700, marginBottom: '0.5rem' }}>You're on Pro now</div>
          <p style={{ color: 'var(--pico-muted-color)' }}>Redirecting to your dashboard…</p>
        </article>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 900, margin: '2rem auto' }}>
      <h2 style={{ marginBottom: '1.5rem' }}>Upgrade to Pro</h2>

      {/* Two-column grid: plan card + payment form */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '1.5rem',
        alignItems: 'start',
      }} className="upgrade-grid">

        {/* Left: Pro plan card */}
        <article style={{
          padding: '1.75rem',
          marginBottom: 0,
          borderLeft: '3px solid var(--pico-primary)',
          background: 'var(--pico-card-background-color)',
        }}>
          <header style={{ marginBottom: '1rem' }}>
            <div style={{
              display: 'inline-block',
              background: 'var(--pico-primary)',
              color: '#fff',
              fontSize: '0.7rem',
              fontWeight: 700,
              letterSpacing: '0.05em',
              textTransform: 'uppercase',
              padding: '2px 8px',
              borderRadius: '4px',
              marginBottom: '0.6rem',
            }}>Pro</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.3rem', marginBottom: '0.4rem' }}>
              <span style={{ fontSize: '2rem', fontWeight: 700 }}>$9</span>
              <span style={{ color: 'var(--pico-muted-color)', fontSize: '0.9rem' }}> / month</span>
            </div>
            <p style={{ color: 'var(--pico-muted-color)', fontSize: '0.85rem', margin: 0 }}>
              Everything you need to supercharge your job search.
            </p>
          </header>

          <ul style={{ listStyle: 'none', padding: 0, margin: '0 0 1.5rem', display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
            {FEATURE_LIST.map(feature => (
              <li key={feature} style={{ display: 'flex', alignItems: 'flex-start', gap: '0.5rem', fontSize: '0.84rem' }}>
                <CheckIcon />
                <span>{feature}</span>
              </li>
            ))}
          </ul>

          <footer style={{ borderTop: '1px solid var(--pico-muted-border-color)', paddingTop: '0.75rem', marginTop: 0 }}>
            <small style={{ color: 'var(--pico-muted-color)', fontSize: '0.72rem' }}>
              Mock billing — no real charge will be made.
            </small>
          </footer>
        </article>

        {/* Right: Payment form */}
        <article style={{ padding: '1.75rem', marginBottom: 0, background: 'var(--pico-card-background-color)' }}>
          <header style={{ marginBottom: '1.25rem' }}>
            <strong style={{ fontSize: '1rem' }}>Payment details</strong>
          </header>

          {apiError && (
            <article style={{ marginBottom: '1rem', padding: '0.75rem 1rem', borderLeft: '3px solid var(--pico-del-color, #ef4444)' }}>
              <p style={{ margin: 0, fontSize: '0.82rem', color: 'var(--pico-del-color, #ef4444)' }}>{apiError}</p>
            </article>
          )}

          <form onSubmit={onSubmit} noValidate>
            {/* Cardholder name */}
            <label>
              Cardholder name
              <input
                type="text"
                value={nameOnCard}
                onChange={e => { setNameOnCard(e.target.value); setErrors(prev => ({ ...prev, nameOnCard: undefined })) }}
                onBlur={() => { if (!nameOnCard.trim()) setErrors(prev => ({ ...prev, nameOnCard: 'Cardholder name is required.' })) }}
                placeholder="Jane Smith"
                autoComplete="cc-name"
                aria-invalid={errors.nameOnCard ? 'true' : undefined}
              />
              {errors.nameOnCard && (
                <small role="alert" style={{ color: 'var(--pico-del-color, #ef4444)' }}>{errors.nameOnCard}</small>
              )}
            </label>

            {/* Card number */}
            <label>
              Card number
              <input
                type="text"
                inputMode="numeric"
                value={cardNumber}
                onChange={handleCardNumberChange}
                onBlur={() => {
                  const raw = cardNumber.replace(/\s/g, '')
                  if (raw.length < 13 || raw.length > 19) setErrors(prev => ({ ...prev, cardNumber: 'Enter a valid card number (13–19 digits).' }))
                  else setErrors(prev => ({ ...prev, cardNumber: undefined }))
                }}
                placeholder="4242 4242 4242 4242"
                autoComplete="cc-number"
                maxLength={19}
                aria-invalid={errors.cardNumber ? 'true' : undefined}
              />
              {errors.cardNumber && (
                <small role="alert" style={{ color: 'var(--pico-del-color, #ef4444)' }}>{errors.cardNumber}</small>
              )}
            </label>

            {/* Exp month + year on one row */}
            <div className="grid" style={{ columnGap: '0.75rem' }}>
              <label>
                Exp month
                <input
                  type="text"
                  inputMode="numeric"
                  value={expMonth}
                  onChange={e => { setExpMonth(e.target.value.replace(/\D/g, '').slice(0, 2)); setErrors(prev => ({ ...prev, expMonth: undefined })) }}
                  onBlur={() => {
                    const m = Number(expMonth)
                    if (!expMonth || isNaN(m) || m < 1 || m > 12) setErrors(prev => ({ ...prev, expMonth: 'Must be 1–12.' }))
                  }}
                  placeholder="12"
                  autoComplete="cc-exp-month"
                  maxLength={2}
                  aria-invalid={errors.expMonth ? 'true' : undefined}
                />
                {errors.expMonth && (
                  <small role="alert" style={{ color: 'var(--pico-del-color, #ef4444)' }}>{errors.expMonth}</small>
                )}
              </label>
              <label>
                Exp year
                <input
                  type="text"
                  inputMode="numeric"
                  value={expYear}
                  onChange={e => { setExpYear(e.target.value.replace(/\D/g, '').slice(0, 4)); setErrors(prev => ({ ...prev, expYear: undefined })) }}
                  onBlur={() => {
                    const y = Number(expYear)
                    const curr = new Date().getFullYear()
                    if (!expYear || isNaN(y) || y < curr || y > curr + 20) setErrors(prev => ({ ...prev, expYear: 'Check the year.' }))
                  }}
                  placeholder="2030"
                  autoComplete="cc-exp-year"
                  maxLength={4}
                  aria-invalid={errors.expYear ? 'true' : undefined}
                />
                {errors.expYear && (
                  <small role="alert" style={{ color: 'var(--pico-del-color, #ef4444)' }}>{errors.expYear}</small>
                )}
              </label>
            </div>

            {/* CVC */}
            <label>
              CVC
              <input
                type="password"
                inputMode="numeric"
                value={cvc}
                onChange={e => { setCvc(e.target.value.replace(/\D/g, '').slice(0, 4)); setErrors(prev => ({ ...prev, cvc: undefined })) }}
                onBlur={() => {
                  if (!cvc || !/^\d{3,4}$/.test(cvc)) setErrors(prev => ({ ...prev, cvc: 'Enter a 3 or 4 digit CVC.' }))
                }}
                placeholder="123"
                autoComplete="cc-csc"
                maxLength={4}
                aria-invalid={errors.cvc ? 'true' : undefined}
              />
              {errors.cvc && (
                <small role="alert" style={{ color: 'var(--pico-del-color, #ef4444)' }}>{errors.cvc}</small>
              )}
            </label>

            <p style={{ fontSize: '0.75rem', color: 'var(--pico-muted-color)', margin: '0.25rem 0 1rem' }}>
              Try <code>4242 4242 4242 4242</code> / <code>12</code> / <code>2030</code> / <code>123</code> / any name.
            </p>

            <button
              type="submit"
              className="contrast"
              disabled={submitting}
              aria-busy={submitting}
              style={{ width: '100%' }}
            >
              {submitting ? 'Processing…' : 'Pay $9 — Upgrade to Pro'}
            </button>
          </form>
        </article>
      </div>

      {/* Responsive: stack on small screens */}
      <style>{`
        @media (max-width: 640px) {
          .upgrade-grid {
            grid-template-columns: 1fr !important;
          }
        }
      `}</style>
    </div>
  )
}
