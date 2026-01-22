'use client';

/**
 * 메시지 상세 모달 컴포넌트
 *
 * 선택된 Kafka 메시지의 상세 정보를 모달로 표시합니다.
 * - 메시지 메타데이터 (토픽, 파티션, 오프셋, 타임스탬프)
 * - 메시지 키
 * - 메시지 값 (JSON 포매팅)
 * - 헤더 목록
 *
 * @module components/message/MessageDetail
 */
import { useCallback, useMemo, useEffect } from 'react';
import {
  X,
  Copy,
  Check,
  Clock,
  Hash,
  Key,
  FileText,
  Tag,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';
import { cn } from '@/lib/utils';
import type { Message } from '@/types';
import { useState } from 'react';

/**
 * MessageDetail Props
 */
interface MessageDetailProps {
  /** 표시할 메시지 */
  message: Message | null;
  /** 모달 열림 상태 */
  open: boolean;
  /** 모달 닫기 핸들러 */
  onClose: () => void;
  /** 클래스명 */
  className?: string;
}

/**
 * 날짜 포맷팅 함수
 */
function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  return date.toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    fractionalSecondDigits: 3,
  });
}

/**
 * JSON 포매팅 시도 함수
 */
function tryFormatJson(value: string | null): { formatted: string; isJson: boolean } {
  if (!value) return { formatted: '(null)', isJson: false };

  try {
    const parsed = JSON.parse(value);
    return { formatted: JSON.stringify(parsed, null, 2), isJson: true };
  } catch {
    return { formatted: value, isJson: false };
  }
}

/**
 * 복사 버튼 컴포넌트
 */
function CopyButton({ text, label }: { text: string; label: string }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      console.error('Failed to copy to clipboard');
    }
  }, [text]);

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={handleCopy}
      className="h-7 px-2"
    >
      {copied ? (
        <>
          <Check className="mr-1 h-3 w-3" />
          Copied
        </>
      ) : (
        <>
          <Copy className="mr-1 h-3 w-3" />
          {label}
        </>
      )}
    </Button>
  );
}

/**
 * 메타데이터 항목 컴포넌트
 */
function MetadataItem({
  icon: Icon,
  label,
  value,
  valueClassName,
}: {
  icon: React.ElementType;
  label: string;
  value: string | number;
  valueClassName?: string;
}) {
  return (
    <div className="flex items-center gap-2">
      <Icon className="h-4 w-4 text-muted-foreground" />
      <span className="text-sm text-muted-foreground">{label}:</span>
      <span className={cn('text-sm font-mono', valueClassName)}>{value}</span>
    </div>
  );
}

/**
 * 메시지 상세 모달 컴포넌트
 *
 * 선택된 Kafka 메시지의 상세 정보를 표시하는 모달입니다.
 *
 * @param props - MessageDetail Props
 * @returns 메시지 상세 모달 컴포넌트
 *
 * @example
 * ```tsx
 * <MessageDetail
 *   message={selectedMessage}
 *   open={!!selectedMessage}
 *   onClose={() => setSelectedMessage(null)}
 * />
 * ```
 */
export function MessageDetail({
  message,
  open,
  onClose,
  className,
}: MessageDetailProps) {
  /** ESC 키로 모달 닫기 */
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && open) {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  /** 모달 열릴 때 body 스크롤 방지 */
  useEffect(() => {
    if (open) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  /** 포맷된 값 */
  const formattedValue = useMemo(() => {
    if (!message) return { formatted: '', isJson: false };
    return tryFormatJson(message.value);
  }, [message]);

  /** 포맷된 키 */
  const formattedKey = useMemo(() => {
    if (!message) return { formatted: '', isJson: false };
    return tryFormatJson(message.key);
  }, [message]);

  if (!open || !message) return null;

  return (
    <>
      {/* 오버레이 */}
      <div
        className="fixed inset-0 bg-black/50 z-50"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* 모달 컨텐츠 */}
      <div
        className={cn(
          'fixed inset-4 md:inset-auto md:left-1/2 md:top-1/2 md:-translate-x-1/2 md:-translate-y-1/2',
          'md:w-[800px] md:max-w-[90vw] md:max-h-[90vh]',
          'bg-background rounded-lg shadow-lg z-50',
          'flex flex-col overflow-hidden',
          className
        )}
        role="dialog"
        aria-modal="true"
        aria-labelledby="message-detail-title"
      >
        {/* 헤더 */}
        <div className="flex items-center justify-between px-6 py-4 border-b">
          <div>
            <h2 id="message-detail-title" className="text-lg font-semibold">
              Message Detail
            </h2>
            <p className="text-sm text-muted-foreground">
              {message.topic} / Partition {message.partition} / Offset{' '}
              {message.offset.toLocaleString()}
            </p>
          </div>
          <Button
            variant="ghost"
            size="icon"
            onClick={onClose}
            className="h-8 w-8"
          >
            <X className="h-4 w-4" />
            <span className="sr-only">Close</span>
          </Button>
        </div>

        {/* 본문 */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* 메타데이터 */}
          <Card>
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Metadata</CardTitle>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <MetadataItem
                icon={Hash}
                label="Topic"
                value={message.topic}
                valueClassName="text-primary"
              />
              <MetadataItem
                icon={Hash}
                label="Partition"
                value={message.partition}
              />
              <MetadataItem
                icon={Hash}
                label="Offset"
                value={message.offset.toLocaleString()}
              />
              <MetadataItem
                icon={Clock}
                label="Timestamp"
                value={formatTimestamp(message.timestamp)}
              />
              <MetadataItem
                icon={Tag}
                label="Timestamp Type"
                value={message.timestampType}
              />
            </CardContent>
          </Card>

          {/* 메시지 키 */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm flex items-center gap-2">
                  <Key className="h-4 w-4" />
                  Key
                  {formattedKey.isJson && (
                    <Badge variant="secondary" className="text-xs">
                      JSON
                    </Badge>
                  )}
                </CardTitle>
                {message.key && (
                  <CopyButton text={message.key} label="Copy Key" />
                )}
              </div>
            </CardHeader>
            <CardContent>
              {message.key ? (
                <pre className="p-4 rounded-md bg-muted font-mono text-sm overflow-x-auto whitespace-pre-wrap break-all">
                  {formattedKey.formatted}
                </pre>
              ) : (
                <p className="text-muted-foreground italic">(null)</p>
              )}
            </CardContent>
          </Card>

          {/* 메시지 값 */}
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <CardTitle className="text-sm flex items-center gap-2">
                  <FileText className="h-4 w-4" />
                  Value
                  {formattedValue.isJson && (
                    <Badge variant="secondary" className="text-xs">
                      JSON
                    </Badge>
                  )}
                </CardTitle>
                {message.value && (
                  <CopyButton text={message.value} label="Copy Value" />
                )}
              </div>
            </CardHeader>
            <CardContent>
              {message.value ? (
                <pre className="p-4 rounded-md bg-muted font-mono text-sm overflow-x-auto whitespace-pre-wrap break-all max-h-80">
                  {formattedValue.formatted}
                </pre>
              ) : (
                <p className="text-muted-foreground italic">(null)</p>
              )}
            </CardContent>
          </Card>

          {/* 헤더 */}
          {message.headers && message.headers.length > 0 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="text-sm flex items-center gap-2">
                  <Tag className="h-4 w-4" />
                  Headers
                  <Badge variant="secondary" className="text-xs">
                    {message.headers.length}
                  </Badge>
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="rounded-md border">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b bg-muted/50">
                        <th className="text-left p-3 font-medium">Key</th>
                        <th className="text-left p-3 font-medium">Value</th>
                      </tr>
                    </thead>
                    <tbody>
                      {message.headers.map((header, index) => (
                        <tr key={index} className="border-b last:border-0">
                          <td className="p-3 font-mono">{header.key}</td>
                          <td className="p-3 font-mono break-all">
                            {header.value}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </CardContent>
            </Card>
          )}
        </div>

        {/* 푸터 */}
        <div className="flex justify-end px-6 py-4 border-t">
          <Button variant="outline" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </>
  );
}

export default MessageDetail;
