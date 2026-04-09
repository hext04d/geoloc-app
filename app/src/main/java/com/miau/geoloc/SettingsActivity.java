package com.miau.geoloc;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import android.widget.RadioGroup;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "geoloc_prefs";
    private static final String KEY_UNITS = "units_system"; // "metric" or "imperial"
    private static final String KEY_LANG = "app_language"; // "pt" or "en"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Configuração de Unidades
        RadioGroup rgUnits = findViewById(R.id.rgUnits);
        String units = prefs.getString(KEY_UNITS, "metric");
        if (units.equals("imperial")) {
            rgUnits.check(R.id.rbImperial);
        } else {
            rgUnits.check(R.id.rbMetric);
        }

        rgUnits.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedUnits = (checkedId == R.id.rbImperial) ? "imperial" : "metric";
            prefs.edit().putString(KEY_UNITS, selectedUnits).apply();
        });

        // Configuração de Idioma
        RadioGroup rgLanguage = findViewById(R.id.rgLanguage);
        String lang = prefs.getString(KEY_LANG, "pt");
        if (lang.equals("en")) {
            rgLanguage.check(R.id.rbLangEn);
        } else {
            rgLanguage.check(R.id.rbLangPt);
        }

        rgLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedLang = (checkedId == R.id.rbLangEn) ? "en" : "pt";
            prefs.edit().putString(KEY_LANG, selectedLang).apply();
            // Nota: Para mudar o idioma em tempo real, seria necessário recriar a activity ou usar um wrapper de contexto.
            // Por enquanto, apenas salvamos a preferência.
        });
    }
}
