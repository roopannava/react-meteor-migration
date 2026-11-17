import { useDispatch, useSelector } from 'react-redux'
import type { TypedUseSelectorHook } from 'react-redux'
import type { RootState, AppDispatch } from '../store'

// Pre-typed versions of the React-Redux hooks. Use these across the app instead
// of the plain `useDispatch` / `useSelector` so dispatch and state are fully typed.
export const useAppDispatch: () => AppDispatch = useDispatch
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector
