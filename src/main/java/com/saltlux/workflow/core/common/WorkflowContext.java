package com.saltlux.workflow.core.common;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import com.saltlux.workflow.core.common.WorkflowExceptions.WorkflowClientException;

import reactor.core.Disposable;

/**
 * 워크플로우 실행의 생명주기를 관리하는 컨텍스트 클래스.
 * <p>
 * 비동기 워크플로우 작업의 상태(취소, 완료)를 추적하고,
 * {@link WorkflowListener}를 통해 이벤트를 전파하며,
 * {@link CompletableFuture}를 통해 결과를 제공한다.
 * </p>
 *
 * @param <T> 워크플로우 결과 타입
 */
public class WorkflowContext<T> {
  /** 워크플로우 실행 결과 */
  private T result;

  /** 비동기 결과를 제공하는 CompletableFuture */
  private final CompletableFuture<T> future = new CompletableFuture<>();

  /** 워크플로우 이벤트를 수신하는 리스너 */
  private final WorkflowListener<T> listener;

  /** 리액티브 스트림 구독을 관리하는 Disposable */
  private Disposable disposable = null;

  /** 워크플로우 취소 여부 */
  private volatile boolean cancelled = false;

  /** 워크플로우 완료 여부 (정상 완료, 에러, 취소 모두 포함) */
  private volatile boolean completed = false;

  /**
   * 취소 시 실행할 콜백.
   * <p>
   * 기본 동작은 CompletableFuture를 취소하는 것이다.
   * {@link #setOnCancel}로 오버라이드하여 취소 시에도 부분 결과를 반환하도록 할 수 있다.
   * </p>
   */
  private Runnable onCancelCallback = () -> {
    completed = true;
    future.cancel(true);
  };

  /**
   * 지정된 리스너로 WorkflowContext를 생성한다.
   *
   * @param listener 워크플로우 이벤트를 수신할 리스너
   */
  public WorkflowContext(final WorkflowListener<T> listener) {
    this.listener = listener;
  }

  /**
   * 워크플로우가 취소되었는지 확인한다.
   *
   * @return 취소된 경우 {@code true}, 그렇지 않으면 {@code false}
   */
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * 워크플로우가 완료(도중에 취소)되었는지 확인하고, 완료된 경우 예외를 던진다.
   *
   * @throws CancellationException 워크플로우가 완료된 경우
   */
  public void checkCompleted() {
    if (cancelled || completed) {
      throw new CancellationException();
    }
  }

  /**
   * 리액티브 스트림 구독을 설정한다.
   * <p>
   * 워크플로우 취소 시 이 Disposable을 통해 구독을 해제한다.
   * </p>
   *
   * @param disposable 설정할 Disposable
   */
  public synchronized void setDisposable(final Disposable disposable) {
    this.disposable = disposable;
  }

  /**
   * 취소 시 실행할 콜백을 설정한다.
   * <p>
   * 기본 동작은 {@code future.cancel(true)}이지만,
   * 이 메서드로 오버라이드하여 취소 시에도 부분 결과를 반환하도록 할 수 있다.
   * 예: {@code context.setOnCancel(() -> context.emitComplete())}
   * </p>
   *
   * @param callback 취소 시 실행할 콜백
   */
  public synchronized void setOnCancel(final Runnable callback) {
    this.onCancelCallback = callback;
  }

  /**
   * 워크플로우를 취소한다.
   * <p>
   * 이미 완료된 워크플로우는 취소할 수 없다.
   * 취소 시 다음 작업이 수행된다:
   * <ul>
   * <li>Disposable이 있으면 구독 해제</li>
   * <li>리스너에 취소 이벤트 전파</li>
   * <li>onCancelCallback 실행 (기본: future.cancel, 오버라이드 시 emitComplete 등)</li>
   * </ul>
   * </p>
   */
  public synchronized void cancel() {
    if (!completed) {
      cancelled = true;
      if (disposable != null) {
        disposable.dispose();
      }
      listener.onCancel();
      onCancelCallback.run();
    }
  }

  /**
   * 다음 결과 항목을 리스너에 전달한다.
   * <p>
   * 이미 완료된 워크플로우에서는 무시된다.
   * </p>
   *
   * @param item 전달할 결과 항목
   */
  public synchronized void emitNext(final T item) {
    if (!completed) {
      listener.onNext(item);
    }
  }

  /**
   * 최종 결과를 설정한다.
   * <p>
   * 이미 완료된 워크플로우에서는 무시된다.
   * {@link #emitComplete()} 호출 시 이 결과가 CompletableFuture에 전달된다.
   * </p>
   *
   * @param result 설정할 결과
   */
  public synchronized void setResult(final T result) {
    if (!completed) {
      this.result = result;
    }
  }

  /**
   * 에러를 발생시키고 워크플로우를 완료한다.
   * <p>
   * 이미 완료된 워크플로우에서는 무시된다.
   * 리스너에 에러 이벤트를 전파하고, CompletableFuture를 예외로 완료한다.
   * </p>
   *
   * @param e 발생한 예외
   */
  public synchronized void emitError(Throwable e) {
    if (!completed) {
      completed = true;
      listener.onError(e);
      future.completeExceptionally(e);
    }
  }

  /**
   * 워크플로우를 정상 완료한다.
   * <p>
   * 이미 완료된 워크플로우에서는 무시된다.
   * 리스너에 완료 이벤트를 전파하고, 설정된 결과로 CompletableFuture를 완료한다.
   * </p>
   */
  public synchronized void emitComplete() {
    if (!completed) {
      completed = true;
      listener.onComplete();
      future.complete(result);
    }
  }

  /**
   * 이 컨텍스트의 CompletableFuture를 반환한다.
   * <p>
   * 반환된 Future를 통해 비동기적으로 결과를 대기하거나,
   * 다른 비동기 작업과 조합할 수 있다.
   * </p>
   *
   * @return 워크플로우 결과를 제공하는 CompletableFuture
   */
  public CompletableFuture<T> toFuture() {
    return future;
  }

  /**
   * 워크플로우 완료를 동기적으로 대기하고 결과를 반환한다.
   * <p>
   * 이 메서드는 워크플로우가 완료될 때까지 현재 스레드를 블로킹한다.
   * </p>
   *
   * @return 워크플로우 실행 결과
   * @throws CancellationException   워크플로우가 취소된 경우
   * @throws WorkflowClientException 인터럽트되거나 예기치 않은 예외가 발생한 경우
   */
  public T get() {
    try {
      return future.get();
    } catch (CancellationException e) {
      throw e;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new WorkflowClientException("interrupted", e);
    } catch (Exception e) {
      throw new WorkflowClientException("unexpected", e);
    }
  }
}
