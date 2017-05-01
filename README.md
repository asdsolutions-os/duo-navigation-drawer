# Duo Navigation Drawer

This Android library provides an easy way to create an alternative navigation
drawer for android. Instead of a drawer that slides over the main content of
the Activity, this lets the content slide away and reveal a menu below it.

By default it applies a scaling effect on the content and menu.

## Demo
[![Demo CountPages alpha](https://j.gifs.com/vgyrrV.gif)](https://www.youtube.com/watch?v=Batgo5dDxyw)

The demo app is included in the `app` module in this project.

## Usage

### 1. Include the library
Add the dependency to your Gradle file:
```
dependencies {
    ...
    compile 'nl.psdcompany:duo-navigation-drawer:1.0.0'
}
```

### 2. Add the `DuoNavigationDrawer` view

1. Add a `DuoNavigationDrawer` to your Activity.

There are two options for custimization of the `DuoNavigationDrawer`. The first option is to customize the layout by adding attributes to the `DuoNavigationDrawer`. The second option is to customize the layout by adding views inside the `DuoNavigationDrawer`. These views need to contain tags in order to be noticed by the menu.

#### Option 1
2. Add the app:content attribute corresponding to your content layout.
3. Add the app:menu attribute corresponding to your menu layout.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/drawer"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:content="@layout/content"
    app:menu="@layout/menu"/>
```

#### Option 2

2. Add a View which contains your menu. Give this the tag `menu`.
3. Add a View which contains your content. Give this the tag `content`.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/drawer"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <nl.psdcompany.duonavigationdrawer.views.DuoMenuView
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:tag="@string/tag_menu"
        app:footer="@layout/view_footer"
        app:header="@layout/view_header"/>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical"
        android:tag="@string/tag_content">

        <android.support.v7.widget.Toolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:background="@color/colorPrimary"
            android:theme="@style/ThemeOverlay.AppCompat.Dark.ActionBar"
            app:popupTheme="@style/ThemeOverlay.AppCompat.Light"/>

        <FrameLayout
            android:id="@+id/container"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:background="@android:color/white"/>
    </LinearLayout>
    
</nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout>
```

### 3. Initialize the drawer view

The API of the `DuoNavigationDrawer` is mostly the same as the original `DrawerLayout` from the Android design library. Same for `DuoDrawerToggle` which is a modified version of the `ActionBarDrawerToggle` to support the `DuoDrawerLayout`.

```Java
DuoDrawerLayout drawerLayout = (DuoDrawerLayout) findViewById(R.id.drawer);
DuoDrawerToggle drawerToggle = new DuoDrawerToggle(this, drawerLayout, toolbar,
        R.string.navigation_drawer_open,
        R.string.navigation_drawer_close);

drawerLayout.setDrawerListener(drawerToggle);
drawerToggle.syncState();
```

### 4. Customize the DuoDrawerLayout

All values are `Float` values. The default values are used in the example.

#### Content scaling effect
The scaling applied on the content when sliding it from left to right.
```xml
app:contentScaleClosed="1.0"
app:contentScaleOpen="0.7"
```

#### Menu scaling effect
The scaling applied on the menu when sliding the content from left to right.
```xml
app:menuScaleClosed="1.1"
app:menuScaleOpen="1.0"
```

#### Menu alpha effect
The alpha on the menu when sliding the content from left to right.
```xml
app:menuAlphaClosed="0.0"
app:menuAlphaOpen="1.0"
```

#### Content margin factor
This value is used to calculate how much of the content should be visible when the content is slided to the right. This is calculated with the width of the `DuoDrawerLayout` when: `getWidth * marginFactor`. So setting this to 1.0f will slide the content out of the activity. The default is 0.7f.

```xml
app:marginFactor="0.7"
```
