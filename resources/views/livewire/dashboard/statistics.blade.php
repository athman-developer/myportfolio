<?php

use App\Models\User;
use Illuminate\Support\Facades\Auth;
use Livewire\Volt\Component;

new class extends Component {
    public function getUsersCountProperty()
    {
        return User::count();
    }

    public function getUsersWithProfilesCountProperty()
    {
        return User::has('profile')->count();
    }

    public function getUsersWithBioCountProperty()
    {
        return User::whereHas('profile', function ($query) {
            $query->whereNotNull('bio')->where('bio', '!=', '');
        })->count();
    }

    public function getUsersWithLocationCountProperty()
    {
        return User::whereHas('profile', function ($query) {
            $query->whereNotNull('location')->where('location', '!=', '');
        })->count();
    }

    public function getUsersWithSocialCountProperty()
    {
        return User::whereHas('profile', function ($query) {
            $query->where(function ($subquery) {
                $subquery->whereNotNull('website')
                    ->orWhereNotNull('twitter')
                    ->orWhereNotNull('github')
                    ->orWhereNotNull('linkedin')
                    ->orWhereNotNull('instagram')
                    ->orWhereNotNull('facebook')
                    ->orWhereNotNull('youtube')
                    ->orWhereNotNull('discord');
            });
        })->count();
    }

    public function getRecentUsersProperty()
    {
        return User::with('profile')
            ->orderBy('created_at', 'desc')
            ->limit(5)
            ->get();
    }

    public function getUsersByLocationProperty()
    {
        return User::selectRaw('profile.location, count(*) as count')
            ->join('profiles as profile', 'users.id', '=', 'profile.user_id')
            ->whereNotNull('profile.location')
            ->where('profile.location', '!=', '')
            ->groupBy('profile.location')
            ->orderBy('count', 'desc')
            ->limit(10)
            ->get();
    }

    public function getUsersByGenderProperty()
    {
        return User::selectRaw('profile.gender, count(*) as count')
            ->join('profiles as profile', 'users.id', '=', 'profile.user_id')
            ->whereNotNull('profile.gender')
            ->where('profile.gender', '!=', '')
            ->groupBy('profile.gender')
            ->orderBy('count', 'desc')
            ->get();
    }

    public function getAverageProfileCompletionProperty()
    {
        $totalUsers = User::count();
        if ($totalUsers === 0) return 0;

        $usersWithProfiles = User::has('profile')->count();
        $usersWithBio = $this->usersWithBioCount;
        $usersWithLocation = $this->usersWithLocationCount;
        $usersWithSocial = $this->usersWithSocialCount;

        // Calculate completion percentage based on key fields
        $completionScore = ($usersWithProfiles + $usersWithBio + $usersWithLocation + $usersWithSocial) / ($totalUsers * 4);
        return round($completionScore * 100);
    }
}; ?>

<div class="max-w-7xl mx-auto px-4 py-8">
    <!-- Page Header -->
    <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Community Statistics</h1>
        <p class="text-gray-600 dark:text-gray-300 mt-2">Overview of the user community and profile statistics</p>
    </div>

    <!-- Key Metrics -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <!-- Total Users -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <div class="flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-600 dark:text-gray-400">Total Users</p>
                    <p class="text-2xl font-bold text-gray-900 dark:text-white mt-1">{{ $usersCount }}</p>
                </div>
                <div class="w-12 h-12 bg-blue-100 dark:bg-blue-900 rounded-lg flex items-center justify-center">
                    <span class="text-blue-600 dark:text-blue-300 text-xl">👥</span>
                </div>
            </div>
        </div>

        <!-- Users with Profiles -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <div class="flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-600 dark:text-gray-400">Users with Profiles</p>
                    <p class="text-2xl font-bold text-gray-900 dark:text-white mt-1">{{ $usersWithProfilesCount }}</p>
                    <p class="text-xs text-gray-500 dark:text-gray-400 mt-1">{{ round(($usersWithProfilesCount / max($usersCount, 1)) * 100) }}% of total</p>
                </div>
                <div class="w-12 h-12 bg-green-100 dark:bg-green-900 rounded-lg flex items-center justify-center">
                    <span class="text-green-600 dark:text-green-300 text-xl">✅</span>
                </div>
            </div>
        </div>

        <!-- Profile Completion -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <div class="flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-600 dark:text-gray-400">Profile Completion</p>
                    <p class="text-2xl font-bold text-gray-900 dark:text-white mt-1">{{ $averageProfileCompletion }}%</p>
                </div>
                <div class="w-12 h-12 bg-purple-100 dark:bg-purple-900 rounded-lg flex items-center justify-center">
                    <span class="text-purple-600 dark:text-purple-300 text-xl">📊</span>
                </div>
            </div>
            <div class="mt-4">
                <div class="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                    <div class="bg-purple-600 h-2 rounded-full" style="width: {{ $averageProfileCompletion }}%"></div>
                </div>
            </div>
        </div>

        <!-- Recent Signups -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <div class="flex items-center justify-between">
                <div>
                    <p class="text-sm font-medium text-gray-600 dark:text-gray-400">Recent Signups (Last 5)</p>
                    <p class="text-sm text-gray-500 dark:text-gray-400 mt-1">Latest community members</p>
                </div>
                <div class="w-12 h-12 bg-orange-100 dark:bg-orange-900 rounded-lg flex items-center justify-center">
                    <span class="text-orange-600 dark:text-orange-300 text-xl">🆕</span>
                </div>
            </div>
        </div>
    </div>

    <!-- Charts and Detailed Stats -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-8">
        <!-- Users by Location -->
        <div class="lg:col-span-2 bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Users by Location</h3>
            <div class="space-y-3">
                @foreach ($usersByLocation as $location)
                    <div class="flex items-center justify-between">
                        <div class="flex items-center space-x-3">
                            <span class="text-gray-400">📍</span>
                            <span class="text-gray-700 dark:text-gray-300">{{ $location->location }}</span>
                        </div>
                        <div class="flex items-center space-x-2">
                            <div class="w-32 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                                <div class="bg-blue-600 h-2 rounded-full" style="width: {{ round(($location->count / max($usersWithLocationCount, 1)) * 100) }}%"></div>
                            </div>
                            <span class="text-sm text-gray-600 dark:text-gray-400">{{ $location->count }}</span>
                        </div>
                    </div>
                @endforeach
                @if ($usersByLocation->isEmpty())
                    <p class="text-gray-500 dark:text-gray-400 text-sm">No location data available</p>
                @endif
            </div>
        </div>

        <!-- Users by Gender -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
            <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Users by Gender</h3>
            <div class="space-y-3">
                @foreach ($usersByGender as $gender)
                    <div class="flex items-center justify-between">
                        <div class="flex items-center space-x-3">
                            @if ($gender->gender === 'male')
                                <span class="text-blue-500">♂</span>
                            @elseif ($gender->gender === 'female')
                                <span class="text-pink-500">♀</span>
                            @else
                                <span class="text-gray-500">👤</span>
                            @endif
                            <span class="text-gray-700 dark:text-gray-300">{{ ucfirst($gender->gender) }}</span>
                        </div>
                        <div class="flex items-center space-x-2">
                            <div class="w-24 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                                <div class="bg-green-600 h-2 rounded-full" style="width: {{ round(($gender->count / max($usersWithProfilesCount, 1)) * 100) }}%"></div>
                            </div>
                            <span class="text-sm text-gray-600 dark:text-gray-400">{{ $gender->count }}</span>
                        </div>
                    </div>
                @endforeach
                @if ($usersByGender->isEmpty())
                    <p class="text-gray-500 dark:text-gray-400 text-sm">No gender data available</p>
                @endif
            </div>
        </div>
    </div>

    <!-- Recent Users -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6">
        <h3 class="text-lg font-semibold text-gray-900 dark:text-white mb-4">Recent Users</h3>
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            @foreach ($recentUsers as $user)
                <a href="{{ route('profile.show', $user) }}" class="block group">
                    <div class="flex items-center space-x-3 p-3 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                        @if ($user->profile && $user->profile->avatar)
                            <img src="{{ Storage::url($user->profile->avatar) }}" alt="{{ $user->name }}" class="w-12 h-12 rounded-full object-cover" />
                        @else
                            <div class="w-12 h-12 bg-gradient-to-br from-blue-400 to-purple-500 rounded-full flex items-center justify-center text-sm font-bold text-white">
                                {{ $user->initials() }}
                            </div>
                        @endif
                        <div class="flex-1 min-w-0">
                            <p class="font-medium text-gray-900 dark:text-white group-hover:text-blue-600">{{ $user->name }}</p>
                            <p class="text-sm text-gray-500 dark:text-gray-400 truncate">{{ $user->email }}</p>
                            @if ($user->profile && $user->profile->location)
                                <p class="text-xs text-gray-400 flex items-center space-x-1 mt-1">
                                    <span>📍</span>
                                    <span>{{ $user->profile->location }}</span>
                                </p>
                            @endif
                        </div>
                    </div>
                </a>
            @endforeach
            @if ($recentUsers->isEmpty())
                <p class="text-gray-500 dark:text-gray-400 text-sm">No recent users</p>
            @endif
        </div>
    </div>
</div>