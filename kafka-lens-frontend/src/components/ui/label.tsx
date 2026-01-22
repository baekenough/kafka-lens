'use client';

/**
 * Label 컴포넌트
 *
 * 폼 필드의 레이블을 표시하는 컴포넌트입니다.
 *
 * @module components/ui/label
 */
import * as React from 'react';
import { cn } from '@/lib/utils';

/**
 * Label Props
 */
export interface LabelProps extends React.LabelHTMLAttributes<HTMLLabelElement> {
  /** 비활성화 스타일 적용 여부 */
  disabled?: boolean;
}

/**
 * Label 컴포넌트
 *
 * @param props - Label Props
 * @returns Label 컴포넌트
 */
const Label = React.forwardRef<HTMLLabelElement, LabelProps>(
  ({ className, disabled, ...props }, ref) => {
    return (
      <label
        ref={ref}
        className={cn(
          'text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70',
          disabled && 'cursor-not-allowed opacity-70',
          className
        )}
        {...props}
      />
    );
  }
);
Label.displayName = 'Label';

export { Label };
