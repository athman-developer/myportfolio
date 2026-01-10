<?php

use Illuminate\Support\Facades\Route;
use Laravel\Fortify\Features;
use Livewire\Volt\Volt;

Route::get('/', function () {
    return view('welcome');
})->name('home');

Route::view('dashboard', 'dashboard')
    ->middleware(['auth', 'verified'])
    ->name('dashboard');

Route::middleware(['auth'])->group(function () {
    Route::redirect('settings', 'settings/profile');

    Volt::route('settings/profile', 'settings.profile')->name('profile.edit');
    Volt::route('settings/password', 'settings.password')->name('user-password.edit');
    Volt::route('settings/appearance', 'settings.appearance')->name('appearance.edit');

    Volt::route('settings/two-factor', 'settings.two-factor')
        ->middleware(
            when(
                Features::canManageTwoFactorAuthentication()
                    && Features::optionEnabled(Features::twoFactorAuthentication(), 'confirmPassword'),
                ['password.confirm'],
                [],
            ),
        )
        ->name('two-factor.show');

    // Profile management route
    Volt::route('settings/profile-management', 'settings.profile-management')->name('profile.management');
});

// Public profile route (accessible to authenticated users)
Route::middleware(['auth'])->group(function () {
    Route::get('profile/{user}', function (App\Models\User $user) {
        return view('livewire.profile.show', ['user' => $user]);
    })->name('profile.show');

    // User directory route
    Route::get('directory', function () {
        return view('livewire.profile.directory');
    })->name('directory');

    // Statistics dashboard route
    Route::get('statistics', function () {
        return view('livewire.dashboard.statistics');
    })->name('statistics');

    // Advanced search route
    Route::get('search', function () {
        return view('livewire.profile.search');
    })->name('search');
});
