<?php

use App\Models\User;
use Illuminate\Support\Facades\Auth;
use Livewire\Attributes\On;
use Livewire\Attributes\Url;
use Livewire\Volt\Component;
use Livewire\WithPagination;

new class extends Component {
    use WithPagination;

    #[Url]
    public string $search = '';
    #[Url]
    public string $location = '';
    #[Url]
    public string $sortBy = 'name';
    #[Url]
    public string $sortDirection = 'asc';

    public function mount(): void
    {
        //
    }

    public function updatingSearch(): void
    {
        $this->resetPage();
    }

    public function updatingLocation(): void
    {
        $this->resetPage();
    }

    public function sortBy($field): void
    {
        if ($this->sortBy === $field) {
            $this->sortDirection = $this->sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            $this->sortDirection = 'asc';
        }

        $this->sortBy = $field;
    }

    public function getUsersProperty()
    {
        return User::with('profile')
            ->when($this->search, function ($query) {
                $query->where(function ($subquery) {
                    $subquery->where('name', 'like', '%' . $this->search . '%')
                        ->orWhere('email', 'like', '%' . $this->search . '%')
                        ->orWhereHas('profile', function ($profileQuery) {
                            $profileQuery->where('bio', 'like', '%' . $this->search . '%')
                                ->orWhere('location', 'like', '%' . $this->search . '%');
                        });
                });
            })
            ->when($this->location, function ($query) {
                $query->whereHas('profile', function ($profileQuery) {
                    $profileQuery->where('location', 'like', '%' . $this->location . '%');
                });
            })
            ->orderBy($this->sortBy, $this->sortDirection)
            ->paginate(12);
    }

    #[On('profile-updated')]
    public function refreshUsers(): void
    {
        $this->resetPage();
    }
}; ?>

<div class="max-w-7xl mx-auto px-4 py-8">
    <!-- Page Header -->
    <div class="mb-8">
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">User Directory</h1>
        <p class="text-gray-600 dark:text-gray-300 mt-2">Discover and connect with other users in the community</p>
    </div>

    <!-- Search and Filters -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div class="md:col-span-2">
                <flux:input wire:model.live="search" :label="__('Search users')" placeholder="Search by name, email, bio, or location..." icon="magnifying-glass" />
            </div>
            <div>
                <flux:input wire:model.live="location" :label="__('Location')" placeholder="City, Country" icon="map-pin" />
            </div>
        </div>

        <!-- Sort Options -->
        <div class="flex items-center justify-between mt-4">
            <div class="flex items-center space-x-4">
                <span class="text-sm text-gray-600 dark:text-gray-300">Sort by:</span>
                <div class="flex items-center space-x-2">
                    <button wire:click="sortBy('name')" class="flex items-center space-x-1 text-sm {{ $sortBy === 'name' ? 'text-blue-600 font-semibold' : 'text-gray-600 dark:text-gray-300' }}">
                        <span>Name</span>
                        @if ($sortBy === 'name')
                            <span>{{ $sortDirection === 'asc' ? '▲' : '▼' }}</span>
                        @endif
                    </button>
                    <button wire:click="sortBy('created_at')" class="flex items-center space-x-1 text-sm {{ $sortBy === 'created_at' ? 'text-blue-600 font-semibold' : 'text-gray-600 dark:text-gray-300' }}">
                        <span>Join Date</span>
                        @if ($sortBy === 'created_at')
                            <span>{{ $sortDirection === 'asc' ? '▲' : '▼' }}</span>
                        @endif
                    </button>
                </div>
            </div>
            <div class="text-sm text-gray-500 dark:text-gray-400">
                Showing {{ $users->firstItem() ?? 0 }}-{{ $users->lastItem() ?? 0 }} of {{ $users->total() }} users
            </div>
        </div>
    </div>

    <!-- User Grid -->
    <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        @foreach ($users as $user)
            <a href="{{ route('profile.show', $user) }}" class="block group">
                <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 hover:shadow-md transition-shadow">
                    <!-- Avatar -->
                    <div class="flex items-center justify-center mb-4">
                        @if ($user->profile && $user->profile->avatar)
                            <img src="{{ Storage::url($user->profile->avatar) }}" alt="{{ $user->name }}" class="w-20 h-20 rounded-full object-cover border-2 border-gray-200 dark:border-gray-600 group-hover:border-blue-400 transition-colors" />
                        @else
                            <div class="w-20 h-20 bg-gradient-to-br from-blue-400 to-purple-500 rounded-full flex items-center justify-center text-2xl font-bold text-white border-2 border-white border-opacity-20">
                                {{ $user->initials() }}
                            </div>
                        @endif
                    </div>

                    <!-- User Info -->
                    <div class="text-center">
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white group-hover:text-blue-600 transition-colors">{{ $user->name }}</h3>
                        @if ($user->profile && $user->profile->show_email)
                            <p class="text-sm text-gray-500 dark:text-gray-400">{{ $user->email }}</p>
                        @else
                            <p class="text-sm text-gray-500 dark:text-gray-400">Email hidden</p>
                        @endif
                        
                        @if ($user->profile && $user->profile->show_location && $user->profile->location)
                            <div class="flex items-center justify-center space-x-2 mt-2">
                                <span class="text-gray-400">📍</span>
                                <span class="text-sm text-gray-600 dark:text-gray-300">{{ $user->profile->location }}</span>
                            </div>
                        @endif

                        @if ($user->profile && $user->profile->bio)
                            <p class="text-sm text-gray-600 dark:text-gray-300 mt-3 line-clamp-3">{{ $user->profile->bio }}</p>
                        @else
                            <p class="text-sm text-gray-500 dark:text-gray-400 mt-3">No bio yet</p>
                        @endif

                        <!-- Social Links Preview -->
                        @if ($user->profile && $user->profile->show_social_links && ($user->profile->twitter || $user->profile->github || $user->profile->linkedin))
                            <div class="flex items-center justify-center space-x-2 mt-4">
                                @if ($user->profile->twitter)
                                    <a href="https://twitter.com/{{ $user->profile->twitter }}" target="_blank" rel="noopener noreferrer" class="text-blue-400 hover:text-blue-600">
                                        🐦
                                    </a>
                                @endif
                                @if ($user->profile->github)
                                    <a href="https://github.com/{{ $user->profile->github }}" target="_blank" rel="noopener noreferrer" class="text-gray-600 hover:text-gray-800 dark:text-gray-300 dark:hover:text-white">
                                        💻
                                    </a>
                                @endif
                                @if ($user->profile->linkedin)
                                    <a href="{{ $user->profile->linkedin }}" target="_blank" rel="noopener noreferrer" class="text-blue-600 hover:text-blue-800">
                                        💼
                                    </a>
                                @endif
                            </div>
                        @endif
                    </div>
                </div>
            </a>
        @endforeach
    </div>

    <!-- Pagination -->
    <div class="mt-8">
        {{ $users->links() }}
    </div>

    @if ($users->isEmpty())
        <div class="text-center py-12">
            <div class="text-gray-400 text-lg">No users found</div>
            <p class="text-gray-500 mt-2">Try adjusting your search criteria</p>
        </div>
    @endif
</div>