# Duo Navigation Drawer [ ![Download](https://api.bintray.com/packages/psdcompany/maven/duo-navigation-drawer/images/download.svg) ](https://bintray.com/psdcompany/maven/duo-navigation-drawer/_latestVersion) [![Build Status](https://travis-ci.org/PSD-Company/duo-navigation-drawer.svg?branch=master)](https://travis-ci.org/PSD-Company/duo-navigation-drawer) [![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-DuoNavigationDrawer-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/5779)

This Android library provides an easy way to create an alternative navigation
drawer for android. Instead of a drawer that slides over the main content of
the Activity, this lets the content slide away and reveal a menu below it.

By default it applies a scaling effect on the content and menu.

## Demo
[![Demo CountPages alpha](https://j.gifs.com/vgyrrV.gif)][2]

The demo app is included in the `app` module in this project.

## Getting Started

### Prerequisites

You can download a jar from GitHub's [releases page][1].

Or use Gradle:

```gradle
repositories {
    mavenCentral() // jcenter() works as well because it pulls from Maven Central
}

dependencies {
    compile 'nl.psdcompany:duo-navigation-drawer:2.0.8'
}
```

Or Maven:

```xml
<dependency>
  <groupId>nl.psdcompany</groupId>
  <artifactId>duo-navigation-drawer</artifactId>
  <version>2.0.8</version>
  <type>pom</type>
</dependency>
```

### Installing


#### 1. Add the `DuoNavigationDrawer` view to your activity
```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    ... />
```

#### 2. Add the `content view` view to your `drawer`
Add the a content view to your drawer by adding the attribute: `app:content` to your drawer.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    app:content="@layout/content"
    ... />
```

or, you can also add a view within the drawer with the tag `content`.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    ... >
    
    <FrameLayout
        android:id="@+id/container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:tag="content"
        ... />

</nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout>
```

#### 3. Add the `menu view` view to your `drawer`
Add the a menu view to your drawer by adding the attribute: `app:menu` to your drawer.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    app:menu="@layout/menu"
    ... />
```

or, you can also add a view within the drawer with the tag `menu`.

```xml
<nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    ... >
    
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:tag="menu"
        ... />

</nl.psdcompany.duonavigationdrawer.views.DuoDrawerLayout>
```

#### 4. Initialize the drawer view

The API of the `DuoNavigationDrawer` is mostly the same as the original `DrawerLayout` from the Android design library. Same for `DuoDrawerToggle` which is a modified version of the `ActionBarDrawerToggle` to support the `DuoDrawerLayout`.

```Java
DuoDrawerLayout drawerLayout = (DuoDrawerLayout) findViewById(R.id.drawer);
DuoDrawerToggle drawerToggle = new DuoDrawerToggle(this, drawerLayout, toolbar,
        R.string.navigation_drawer_open,
        R.string.navigation_drawer_close);

drawerLayout.setDrawerListener(drawerToggle);
drawerToggle.syncState();
```

## Customization

### Using the `DuoMenuView`

If you want your `menu` to look like the demo. you should consider using the `DuoMenuView` For more info using the `DuoMenuView` click [here][3].

### Effects

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

#### Click to close surface scaling effect
The scaling applied on the click to close surface when the drawer is open.
```xml
app:clickToCloseScale="0.7"
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
## Apps using the DuoNavigationDrawer

* [Bagels & Beans](https://play.google.com/store/apps/details?id=nl.dtt.bagelsbeans)
* [Hebban](https://play.google.com/store/apps/details?id=nl.dtt.hebban)

Feel free to apply your app to the list by sending me an email with a link to your app in the play store.

## Developed By

* Alexander Pot - <alexander.dpot@gmail.com>

## Donations
If you'd like to support DuoNavigationDrawer development, you could make some donations here:

* [![Donate](https://img.shields.io/badge/Donate-PayPal-green.svg)](http://www.paypal.me/alexanderpot)

Thank you very much in advance!

## License

    Copyright 2017 Alexander Pot

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[1]: https://github.com/PSD-Company/duo-navigation-drawer/releases
[2]: https://www.youtube.com/watch?v=Batgo5dDxyw
[3]: https://github.com/PSD-Company/duo-navigation-drawer/blob/master/dev/MENU_VIEW.md
