import clsx from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Compose Tailwind class names safely, deduplicating conflicts. */
export const cn = (...args) => twMerge(clsx(...args))
