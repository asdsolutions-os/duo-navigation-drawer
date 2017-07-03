package nl.psdcompany.duonavigationdrawer.example;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

import nl.psdcompany.duonavigationdrawer.views.CustomDuoDrawer;
import nl.psdcompany.duonavigationdrawer.views.DuoMenuView;

public class MainActivity extends AppCompatActivity implements DuoMenuView.OnMenuClickListener {
    private CustomDuoDrawer mDrawerLayout;

    private MenuAdapter mMenuAdapter;
    private ViewHolder mViewHolder;

    private ArrayList<String> mTitles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTitles = new ArrayList<>(Arrays.asList(getResources().getStringArray(R.array.menuOptions)));

        // Initialize the views
        mViewHolder = new ViewHolder();

        // Handle toolbar actions
        handleToolbar();

        // Handle menu actions
        handleMenu();

        // Handle drawer actions
        handleDrawer();

        // Show main fragment in container
        goToFragment(new MainFragment(), false);
        mMenuAdapter.setViewSelected(0, true);
        setTitle(mTitles.get(0));
    }

    private void handleToolbar() {
        setSupportActionBar(mViewHolder.mToolbar);
    }

    private void handleDrawer() {
        mDrawerLayout = (CustomDuoDrawer) findViewById(R.id.drawer_layout);
        setSupportActionBar(mViewHolder.mToolbar);
    }

    private void handleMenu() {
        mMenuAdapter = new MenuAdapter(mTitles);

//        mViewHolder.mDuoMenuView.setOnMenuClickListener(this);
//        mViewHolder.mDuoMenuView.setAdapter(mMenuAdapter);
    }

    private void goToFragment(Fragment fragment, boolean addToBackStack) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if (addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.add(R.id.container, fragment).commit();
    }

    @Override
    public void onFooterClicked() {
        Toast.makeText(this, "onFooterClicked", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onHeaderClicked() {
        Toast.makeText(this, "onHeaderClicked", Toast.LENGTH_SHORT).show();
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

        // Close the drawer
//        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    private class ViewHolder {
//        private DuoMenuView mDuoMenuView;
        private Toolbar mToolbar;

        ViewHolder() {
            mToolbar = (Toolbar) findViewById(R.id.toolbar);
//            mDuoMenuView = (DuoMenuView) findViewById(R.id.menu);
        }
    }
}
