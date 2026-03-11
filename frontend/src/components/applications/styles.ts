import type React from 'react'

export const cellStyle: React.CSSProperties = {
  fontSize: '0.78rem',
  padding: '4px 7px',
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  maxWidth: '150px',
}

export const roleCell: React.CSSProperties = {
  ...cellStyle,
  maxWidth: '280px',
}

export const thStyle: React.CSSProperties = {
  fontSize: '0.7rem',
  fontWeight: 600,
  padding: '4px 7px',
  textTransform: 'uppercase',
  letterSpacing: '0.05em',
  whiteSpace: 'nowrap',
}

export const filterInput: React.CSSProperties = {
  fontSize: '0.78rem',
  padding: '3px 6px',
  height: 'auto',
  marginBottom: 0,
}
