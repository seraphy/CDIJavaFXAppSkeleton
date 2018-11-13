#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.ui.common;

import java.util.Objects;

import javafx.scene.Parent;

/**
 * JavaFXのコンテナを作成するための抽象基底クラス.<br>
 */
public abstract class AbstractDocumentController {

	/**
	 * ルートとなるJavaFXコンテナ要素
	 */
	private Parent root;

	/**
	 * ルートとなるJavaFXコンテナ要素を(まだ作成していなければ)作成する.
	 */
	protected abstract void makeRoot();

	/**
	 * ルートとなるJavaFXコンテナ要素を取得する.<br>
	 * まだ作成されていない場合は作成される.<br>
	 * @return
	 */
	public Parent getRoot() {
		if (root == null) {
			makeRoot();
			assert root != null;

			// 作成したParentのuserDataに、
			// このコントローラのインスタンスを設定する.
			root.setUserData(this);
		}
		return root;
	}

	/**
	 * ルートとなるJavaFXコンテナ要素を設定する.
	 * @param root
	 */
	protected final void setRoot(Parent root) {
		Objects.requireNonNull(root);
		this.root = root;
	}
}
