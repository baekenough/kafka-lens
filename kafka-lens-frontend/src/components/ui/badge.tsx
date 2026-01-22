import * as React from 'react';
import { cva, type VariantProps } from 'class-variance-authority';

import { cn } from '@/lib/utils';

/**
 * 뱃지 스타일 변형 정의
 */
const badgeVariants = cva(
  // 기본 스타일
  'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2',
  {
    variants: {
      variant: {
        default:
          'border-transparent bg-primary text-primary-foreground hover:bg-primary/80',
        secondary:
          'border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80',
        destructive:
          'border-transparent bg-destructive text-destructive-foreground hover:bg-destructive/80',
        outline: 'text-foreground',
        // Kafka 상태용 커스텀 변형
        healthy:
          'border-transparent bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200',
        warning:
          'border-transparent bg-yellow-100 text-yellow-800 dark:bg-yellow-900 dark:text-yellow-200',
        error:
          'border-transparent bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
        offline:
          'border-transparent bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300',
      },
    },
    defaultVariants: {
      variant: 'default',
    },
  }
);

/**
 * 뱃지 컴포넌트 Props
 */
export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

/**
 * 뱃지 컴포넌트
 * 상태, 카운트, 라벨 등을 표시하는 데 사용
 *
 * @example
 * // 기본 뱃지
 * <Badge>NEW</Badge>
 *
 * // Kafka 상태 뱃지
 * <Badge variant="healthy">Running</Badge>
 * <Badge variant="error">Offline</Badge>
 */
function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
