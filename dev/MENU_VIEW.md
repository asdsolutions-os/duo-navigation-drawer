# Duo Menu View

This `menu view` is made to recreate the [demo][1] more easely.

<img src="https://github.com/PSD-Company/duo-navigation-drawer/blob/master/dev/menuView.png" width="250">

#### 1. Add the `DuoMenuView` to your `drawer`.
```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    ... >

    <nl.psdcompany.duonavigationdrawer.views.DuoMenuView
        xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@+id/menu"
        android:tag="menu"
        ... />
        
</nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout>
```

#### 2. Add the your own header and footer

Add your own header and footer view to the `DuoMenuView` using the attributes `app:footer` and `app:header`.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoMenuView
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:tag="@string/tag_menu"
    app:footer="@layout/view_footer"
    app:header="@layout/view_header"
    ... />
```

#### 3. Initialize the `menu view`.

Create your own [menu adapter][2] and initialize the `menu view`.

```Java
DuoMenuView duoMenuView = (DuoMenuView) findViewById(R.id.menu);
MenuAdapter menuAdapter = new MenuAdapter(mMenuOptions);
duoMenuView.setAdapter(menuAdapter);
```

#### 4. Start listening to events.
```Java
duoMenuView.setOnMenuClickListener(new DuoMenuView.OnMenuClickListener() {
    @Override
    public void onFooterClicked() {
        // If the footer view contains a button
        // it will launch this method on the button click. 
        // If the view does not contain a button it will listen
        // to the root view click.
    }

    @Override
    public void onHeaderClicked() {

    }

    @Override
    public void onOptionClicked(int position, Object objectClicked) {
            // Set the toolbar title
        setTitle(mTitles.get(position));

        // Set the right options selected
        mMenuAdapter.setViewSelected(position, true);

        // Navigate to the right fragment
        switch (position) {
            default:
                goToFragment(new MainFragment(), false);
                break;
    }
});
```

#### 5. Using the `DuoOptionView` (Optional)
Using the `DuoOptionView` to fill the `DuoMenuView`. You can see how it's used [here][2].

<img src="https://github.com/PSD-Company/duo-navigation-drawer/blob/master/dev/optionViews.png" width="250">

[1]: https://github.com/PSD-Company/duo-navigation-drawer/blob/master/README.md#demo
[2]: https://github.com/PSD-Company/duo-navigation-drawer/blob/master/app/src/main/java/nl/psdcompany/duonavigationdrawer/example/MenuAdapter.java
