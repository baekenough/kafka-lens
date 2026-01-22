'use client';

/**
 * Select 컴포넌트
 *
 * 네이티브 select 기반의 간단한 드롭다운 컴포넌트입니다.
 * 복잡한 기능이 필요한 경우 @radix-ui/react-select 설치를 권장합니다.
 *
 * @module components/ui/select
 */
import * as React from 'react';
import { ChevronDown } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * Select Context
 */
interface SelectContextValue {
  value: string;
  onValueChange: (value: string) => void;
  disabled?: boolean;
}

const SelectContext = React.createContext<SelectContextValue | null>(null);

/**
 * Select Props
 */
interface SelectProps {
  /** 현재 선택된 값 */
  value?: string;
  /** 값 변경 핸들러 */
  onValueChange?: (value: string) => void;
  /** 비활성화 여부 */
  disabled?: boolean;
  /** 자식 컴포넌트 */
  children: React.ReactNode;
}

/**
 * Select 루트 컴포넌트
 */
function Select({ value = '', onValueChange = () => {}, disabled, children }: SelectProps) {
  return (
    <SelectContext.Provider value={{ value, onValueChange, disabled }}>
      {children}
    </SelectContext.Provider>
  );
}

/**
 * SelectTrigger Props
 */
interface SelectTriggerProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  /** 자식 컴포넌트 */
  children: React.ReactNode;
}

/**
 * SelectTrigger 컴포넌트
 */
const SelectTrigger = React.forwardRef<HTMLButtonElement, SelectTriggerProps>(
  ({ className, children, ...props }, ref) => {
    const context = React.useContext(SelectContext);
    const [open, setOpen] = React.useState(false);
    const triggerRef = React.useRef<HTMLDivElement>(null);

    React.useEffect(() => {
      const handleClickOutside = (event: MouseEvent) => {
        if (triggerRef.current && !triggerRef.current.contains(event.target as Node)) {
          setOpen(false);
        }
      };

      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
      <div ref={triggerRef} className="relative">
        <button
          ref={ref}
          type="button"
          className={cn(
            'flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50',
            className
          )}
          disabled={context?.disabled}
          onClick={() => setOpen(!open)}
          {...props}
        >
          {children}
          <ChevronDown className="h-4 w-4 opacity-50" />
        </button>
        {open && (
          <SelectContentInternal onClose={() => setOpen(false)} />
        )}
      </div>
    );
  }
);
SelectTrigger.displayName = 'SelectTrigger';

/** 내부적으로 SelectContent의 children을 저장할 변수 */
let selectContentChildren: React.ReactNode = null;

/**
 * SelectContent Props
 */
interface SelectContentProps {
  /** 자식 컴포넌트 (SelectItem 목록) */
  children: React.ReactNode;
}

/**
 * SelectContent 컴포넌트
 *
 * 드롭다운 메뉴의 컨텐츠 영역입니다.
 */
function SelectContent({ children }: SelectContentProps) {
  selectContentChildren = children;
  return null;
}

/**
 * 내부 SelectContent 렌더링 컴포넌트
 */
function SelectContentInternal({ onClose }: { onClose: () => void }) {
  return (
    <div className="absolute z-50 mt-1 w-full overflow-hidden rounded-md border bg-popover text-popover-foreground shadow-md">
      <div className="p-1" onClick={onClose}>
        {selectContentChildren}
      </div>
    </div>
  );
}

/**
 * SelectValue Props
 */
interface SelectValueProps {
  /** 플레이스홀더 텍스트 */
  placeholder?: string;
}

/**
 * SelectValue 컴포넌트
 *
 * 현재 선택된 값을 표시합니다.
 */
function SelectValue({ placeholder }: SelectValueProps) {
  const context = React.useContext(SelectContext);
  const [displayValue, setDisplayValue] = React.useState<string>('');

  React.useEffect(() => {
    // SelectItem의 children에서 텍스트 추출
    if (context?.value && selectContentChildren) {
      const items = React.Children.toArray(selectContentChildren);
      const selectedItem = items.find((item) => {
        if (React.isValidElement(item)) {
          return item.props.value === context.value;
        }
        return false;
      });
      if (selectedItem && React.isValidElement(selectedItem)) {
        setDisplayValue(selectedItem.props.children as string);
      }
    } else {
      setDisplayValue('');
    }
  }, [context?.value]);

  return (
    <span className={cn(!displayValue && 'text-muted-foreground')}>
      {displayValue || placeholder}
    </span>
  );
}

/**
 * SelectItem Props
 */
interface SelectItemProps {
  /** 항목의 값 */
  value: string;
  /** 표시할 텍스트 */
  children: React.ReactNode;
  /** 비활성화 여부 */
  disabled?: boolean;
}

/**
 * SelectItem 컴포넌트
 *
 * 드롭다운의 개별 항목입니다.
 */
function SelectItem({ value, children, disabled }: SelectItemProps) {
  const context = React.useContext(SelectContext);
  const isSelected = context?.value === value;

  return (
    <div
      className={cn(
        'relative flex w-full cursor-default select-none items-center rounded-sm py-1.5 px-2 text-sm outline-none',
        'hover:bg-accent hover:text-accent-foreground',
        isSelected && 'bg-accent text-accent-foreground',
        disabled && 'pointer-events-none opacity-50'
      )}
      onClick={() => {
        if (!disabled) {
          context?.onValueChange(value);
        }
      }}
    >
      {children}
    </div>
  );
}

export { Select, SelectTrigger, SelectContent, SelectValue, SelectItem };
