Gallery Multi Picker
========

> [!CAUTION]
> Deprecated! This library is no longer maintained.  
> Please consider using official [photo picker](https://developer.android.com/training/data-storage/shared/photo-picker) instead.

ギャラリーから複数の画像を取得するActivityのライブラリです。
Twitterクライアントで複数添付を行うために作ったものをライブラリ化しました。

## How to Use
* build.gradleのdependenciesにいい感じに加えてください。よく知らないです。
* 利用元のAndroidManifest.xmlのapplications内に本ライブラリのActivityを加えてください。
```
<activity android:name="info.shibafu528.gallerymultipicker.MultiPickerActivity" android:theme="@style/Theme.AppCompat">
    <meta-data
        android:name="info.shibafu528.gallerymultipicker.ICON_THEME"
        android:value="dark"/>
</activity>
```
ActionBarActivityを使用していますので、Theme.AppCompat系が必須です。
meta-dataはActionBar上のアイコンの配色を指定します。```light```か```dark```のどちらかを選択してください。
meta-dataが無い場合は```light```にしたものとなります。
* startActivityForResult()で呼び出します。呼び出しの際のIntentの作成は、MultiPickerActivityにいくつかのヘルパーメソッドがあります。
```
//第2引数で選択可能な数を指定。PICK_LIMIT_INFINITYで無制限。
Intent intent = MultiPickerActivity.newIntent(getApplicationContext(), MultiPickerActivity.PICK_LIMIT_INFINITY);
startActivityForResult(intent, REQUEST_CODE);
```
* onActivityResult()で結果を受け取ります。選択された画像のUriがParcelable Arrayとして格納されています。
Uri[]にキャストすることでいい感じに使えますが、いい方法が無い場合はMultiPickerActivityのヘルパーメソッドをお使いください。
```
if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
    Uri[] pickedUris = MultiPickerActivity.getPickedUris(data);
    // pickedUrisに格納されているので利用する
}
```


--------

なんか実装汚いので、これより良いライブラリが既存であったら教えて欲しいです。