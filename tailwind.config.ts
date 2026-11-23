import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './app/**/*.{ts,tsx}',
    './components/**/*.{ts,tsx}',
    './lib/**/*.{ts,tsx}',
    './hooks/**/*.{ts,tsx}',
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        ink: {
          950: '#05080F',
          900: '#090D16',
          850: '#0C1220',
          800: '#111A2B',
          700: '#182338',
          600: '#22304A',
        },
        neon: {
          DEFAULT: '#06B6D4',
          soft: '#22D3EE',
          deep: '#0891B2',
        },
        gold: {
          DEFAULT: '#F59E0B',
          soft: '#FBBF24',
          deep: '#B45309',
        },
        danger: '#F43F5E',
        success: '#34D399',
      },
      fontFamily: {
        sans: ['var(--font-kurdish)', 'Noto Sans Arabic', 'Vazirmatn', 'system-ui', 'sans-serif'],
        display: ['var(--font-kurdish)', 'Noto Sans Arabic', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        '2xl': '1rem',
        '3xl': '1.5rem',
        '4xl': '2rem',
      },
      boxShadow: {
        glass: '0 18px 50px -24px rgba(0, 0, 0, 0.9), inset 0 1px 0 0 rgba(255, 255, 255, 0.05)',
        neon: '0 0 0 1px rgba(6, 182, 212, 0.35), 0 12px 40px -14px rgba(6, 182, 212, 0.65)',
        gold: '0 0 0 1px rgba(245, 158, 11, 0.35), 0 12px 40px -14px rgba(245, 158, 11, 0.6)',
      },
      backgroundImage: {
        'aurora':
          'radial-gradient(90rem 50rem at 85% -10%, rgba(6, 182, 212, 0.18), transparent 60%), radial-gradient(70rem 40rem at 0% 100%, rgba(245, 158, 11, 0.12), transparent 60%)',
      },
      keyframes: {
        'fade-up': {
          from: { opacity: '0', transform: 'translateY(10px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'sheet-up': {
          from: { transform: 'translateY(4%)', opacity: '0' },
          to: { transform: 'translateY(0)', opacity: '1' },
        },
        'pulse-ring': {
          '0%': { transform: 'scale(0.85)', opacity: '0.7' },
          '70%': { transform: 'scale(1.6)', opacity: '0' },
          '100%': { transform: 'scale(1.6)', opacity: '0' },
        },
        shimmer: {
          '100%': { transform: 'translateX(-200%)' },
        },
      },
      animation: {
        'fade-up': 'fade-up 0.35s ease-out both',
        'sheet-up': 'sheet-up 0.3s cubic-bezier(0.16, 1, 0.3, 1) both',
        'pulse-ring': 'pulse-ring 2s ease-out infinite',
        shimmer: 'shimmer 1.8s linear infinite',
      },
    },
  },
  plugins: [],
};

export default config;
