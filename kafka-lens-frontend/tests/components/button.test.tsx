/**
 * Button 컴포넌트 테스트
 */
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Button } from '@/components/ui/button';

describe('Button', () => {
  it('텍스트를 올바르게 렌더링한다', () => {
    render(<Button>클릭</Button>);

    expect(screen.getByRole('button', { name: '클릭' })).toBeInTheDocument();
  });

  it('클릭 이벤트를 처리한다', async () => {
    const handleClick = jest.fn();
    const user = userEvent.setup();

    render(<Button onClick={handleClick}>클릭</Button>);

    await user.click(screen.getByRole('button'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('disabled 상태에서 클릭되지 않는다', async () => {
    const handleClick = jest.fn();
    const user = userEvent.setup();

    render(
      <Button onClick={handleClick} disabled>
        클릭
      </Button>
    );

    await user.click(screen.getByRole('button'));

    expect(handleClick).not.toHaveBeenCalled();
  });

  it('variant에 따라 올바른 스타일이 적용된다', () => {
    const { rerender } = render(<Button variant="default">기본</Button>);
    expect(screen.getByRole('button')).toHaveClass('bg-primary');

    rerender(<Button variant="destructive">삭제</Button>);
    expect(screen.getByRole('button')).toHaveClass('bg-destructive');

    rerender(<Button variant="outline">아웃라인</Button>);
    expect(screen.getByRole('button')).toHaveClass('border');
  });

  it('size에 따라 올바른 크기가 적용된다', () => {
    const { rerender } = render(<Button size="default">기본</Button>);
    expect(screen.getByRole('button')).toHaveClass('h-10');

    rerender(<Button size="sm">작은</Button>);
    expect(screen.getByRole('button')).toHaveClass('h-9');

    rerender(<Button size="lg">큰</Button>);
    expect(screen.getByRole('button')).toHaveClass('h-11');
  });
});
