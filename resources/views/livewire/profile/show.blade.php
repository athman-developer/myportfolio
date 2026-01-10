<?php

use App\Models\User;
use Illuminate\Support\Facades\Auth;
use Livewire\Attributes\On;
use Livewire\Volt\Component;

new class extends Component {
    public User $user;
    public bool $isOwnProfile = false;

    /**
     * Mount the component.
     */
    public function mount(User $user): void
    {
        $this->user = $user->load('profile');
        $this->isOwnProfile = Auth::check() && Auth::id() === $user->id;
    }

    /**
     * Handle profile updates from the management component.
     */
    #[On('profile-updated')]
    public function refreshProfile(): void
    {
        $this->user->refresh();
        $this->user->load('profile');
    }
}; ?>

<div class="max-w-4xl mx-auto px-4 py-8">
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden">
        <!-- Profile Header -->
        <div class="bg-gradient-to-r from-blue-600 to-purple-600 px-6 py-8 text-white">
            <div class="flex items-center justify-between">
                <div class="flex items-center space-x-4">
                    @if ($user->profile && $user->profile->avatar)
                        <img src="{{ Storage::url($user->profile->avatar) }}" alt="{{ $user->name }}" class="w-20 h-20 rounded-full border-2 border-white border-opacity-30 object-cover" />
                    @else
                        <div class="w-20 h-20 bg-white bg-opacity-20 rounded-full flex items-center justify-center text-2xl font-bold border-2 border-white border-opacity-30">
                            {{ $user->initials() }}
                        </div>
                    @endif
                    <div>
                        <h1 class="text-2xl font-bold">{{ $user->name }}</h1>
                        @if ($user->profile && $user->profile->show_email)
                            <p class="text-blue-100">{{ $user->email }}</p>
                        @else
                            <p class="text-blue-100">Email hidden by user</p>
                        @endif
                        @if ($user->profile && $user->profile->show_location && $user->profile->location)
                            <p class="text-blue-100 mt-1">📍 {{ $user->profile->location }}</p>
                        @endif
                    </div>
                </div>
                <div class="flex items-center space-x-3">
                    @if ($isOwnProfile)
                        <a href="{{ route('profile.management') }}" class="bg-white text-blue-600 px-4 py-2 rounded-lg font-semibold hover:bg-gray-100 transition-colors">
                            Edit Profile
                        </a>
                    @endif
                </div>
            </div>
        </div>

        <!-- Profile Content -->
        <div class="p-6">
            @if ($user->profile && $user->profile->bio)
                <div class="mb-6">
                    <h2 class="text-lg font-semibold text-gray-900 dark:text-white mb-2">About</h2>
                    <p class="text-gray-600 dark:text-gray-300">{{ $user->profile->bio }}</p>
                </div>
            @endif

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <!-- Personal Information -->
                <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <h3 class="font-semibold text-gray-900 dark:text-white mb-3">Personal Information</h3>
                    <div class="space-y-2">
                        @if ($user->profile && $user->profile->phone)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">📱</span>
                                <span class="text-gray-700 dark:text-gray-300">{{ $user->profile->phone }}</span>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->birthday)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">🎂</span>
                                <span class="text-gray-700 dark:text-gray-300">{{ $user->profile->birthday }}</span>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->gender)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">👤</span>
                                <span class="text-gray-700 dark:text-gray-300">{{ ucfirst($user->profile->gender) }}</span>
                            </div>
                        @endif
                    </div>
                </div>

                <!-- Social Links -->
                <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <h3 class="font-semibold text-gray-900 dark:text-white mb-3">Social Links</h3>
                    <div class="space-y-2">
                        @if ($user->profile && $user->profile->website)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">🌐</span>
                                <a href="{{ $user->profile->website }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    Website
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->twitter)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">🐦</span>
                                <a href="https://twitter.com/{{ $user->profile->twitter }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    @{{ $user->profile->twitter }}
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->github)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">💻</span>
                                <a href="https://github.com/{{ $user->profile->github }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    GitHub: {{ $user->profile->github }}
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->linkedin)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">💼</span>
                                <a href="{{ $user->profile->linkedin }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    LinkedIn
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->instagram)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">📸</span>
                                <a href="https://instagram.com/{{ $user->profile->instagram }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    @{{ $user->profile->instagram }}
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->facebook)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">📘</span>
                                <a href="https://facebook.com/{{ $user->profile->facebook }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    Facebook
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->youtube)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">📺</span>
                                <a href="{{ $user->profile->youtube }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800 dark:text-blue-400">
                                    YouTube
                                </a>
                            </div>
                        @endif
                        @if ($user->profile && $user->profile->discord)
                            <div class="flex items-center space-x-2">
                                <span class="text-gray-500">🎮</span>
                                <span class="text-gray-700 dark:text-gray-300">{{ $user->profile->discord }}</span>
                            </div>
                        @endif
                    </div>
                </div>
            </div>

            @if ($user->profile && $user->profile->description)
                <div class="mt-6 bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                    <h3 class="font-semibold text-gray-900 dark:text-white mb-3">Description</h3>
                    <p class="text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{{ $user->profile->description }}</p>
                </div>
            @endif
        </div>
    </div>
</div>