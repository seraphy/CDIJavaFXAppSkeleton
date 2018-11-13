package jp.seraphyware.javafxexam.jfxexam1.ui.util;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;

public interface ProgressInfo {

	StringExpression titleProperty();

	StringExpression messageProperty();

	DoubleExpression progressProperty();

	void init(Runnable callbackOnStart);

	boolean cancel();

	static ProgressInfo prompt(String title, String message, BooleanSupplier cancelCallback) {
		StringProperty titleProperty = new SimpleStringProperty(title);
		StringProperty messageProperty = new SimpleStringProperty(message);
		DoubleProperty progressProperty = new SimpleDoubleProperty(-1d);
		return new ProgressInfo() {
			@Override
			public StringExpression titleProperty() {
				return titleProperty;
			}

			@Override
			public StringExpression messageProperty() {
				return messageProperty;
			}

			@Override
			public DoubleExpression progressProperty() {
				return progressProperty;
			}

			@Override
			public void init(Runnable callbackOnStart) {
				if (callbackOnStart != null) {
					callbackOnStart.run();
				}
			}

			@Override
			public boolean cancel() {
				if (cancelCallback != null) {
					return cancelCallback.getAsBoolean();
				}
				return true;
			}
		};
	}

	static ProgressInfo adapt(Task<?> bgTask) {
		return new ProgressInfo() {
			@Override
			public StringExpression titleProperty() {
				return bgTask.titleProperty();
			}

			@Override
			public StringExpression messageProperty() {
				return bgTask.messageProperty();
			}

			@Override
			public DoubleExpression progressProperty() {
				return bgTask.progressProperty();
			}

			@Override
			public void init(Runnable callbackOnStart) {
				AtomicBoolean invoked = new AtomicBoolean();
				Runnable r = () -> {
					if (invoked.compareAndSet(false, true)) {
						if (callbackOnStart != null) {
							callbackOnStart.run();
						}
					}
				};
				bgTask.setOnRunning(evt -> r.run());
				if (bgTask.getState().equals(State.RUNNING)) {
					r.run();
				}
			}

			@Override
			public boolean cancel() {
				return bgTask.cancel();
			}
		};
	}
}