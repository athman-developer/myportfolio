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
    public string $query = '';
    #[Url]
    public string $location = '';
    #[Url]
    public string $gender = '';
    #[Url]
    public string $sortBy = 'relevance';
    #[Url]
    public string $sortDirection = 'desc';

    public function mount(): void
    {
        //
    }

    public function updatingQuery(): void
    {
        $this->resetPage();
    }

    public function updatingLocation(): void
    {
        $this->resetPage();
    }

    public function updatingGender(): void
    {
        $this->resetPage();
    }

    public function sortBy($field): void
    {
        if ($this->sortBy === $field) {
            $this->sortDirection = $this->sortDirection === 'asc' ? 'desc' : 'asc';
        } else {
            $this->sortDirection = $field === 'relevance' ? 'desc' : 'asc';
        }

        $this->sortBy = $field;
    }

    public function getUsersProperty()
    {
        $query = User::with('profile');

        // Apply search query
        if ($this->query) {
            $searchTerms = explode(' ', $this->query);
            $query->where(function ($subquery) use ($searchTerms) {
                foreach ($searchTerms as $term) {
                    $subquery->where(function ($innerQuery) use ($term) {
                        $innerQuery->where('name', 'like', '%' . $term . '%')
                            ->orWhere('email', 'like', '%' . $term . '%')
                            ->orWhereHas('profile', function ($profileQuery) use ($term) {
                                $profileQuery->where('bio', 'like', '%' . $term . '%')
                                    ->orWhere('location', 'like', '%' . $term . '%')
                                    ->orWhere('description', 'like', '%' . $term . '%')
                                    ->orWhere('website', 'like', '%' . $term . '%')
                                    ->orWhere('twitter', 'like', '%' . $term . '%')
                                    ->orWhere('github', 'like', '%' . $term . '%')
                                    ->orWhere('linkedin', 'like', '%' . $term . '%')
                                    ->orWhere('instagram', 'like', '%' . $term . '%')
                                    ->orWhere('facebook', 'like', '%' . $term . '%')
                                    ->orWhere('youtube', 'like', '%' . $term . '%')
                                    ->orWhere('discord', 'like', '%' . $term . '%');
                            });
                    });
                }
            });
        }

        // Apply location filter
        if ($this->location) {
            $query->whereHas('profile', function ($profileQuery) {
                $profileQuery->where('location', 'like', '%' . $this->location . '%');
            });
        }

        // Apply gender filter
        if ($this->gender) {
            $query->whereHas('profile', function ($profileQuery) {
                $profileQuery->where('gender', $this->gender);
            });
        }

        // Apply sorting
        if ($this->sortBy === 'relevance' && $this->query) {
            // Custom relevance scoring
            $query->select('users.*')
                ->selectRaw('(
                    CASE WHEN users.name LIKE ? THEN 10 ELSE 0 END +
                    CASE WHEN users.email LIKE ? THEN 5 ELSE 0 END +
                    CASE WHEN profiles.bio LIKE ? THEN 8 ELSE 0 END +
                    CASE WHEN profiles.location LIKE ? THEN 6 ELSE 0 END +
                    CASE WHEN profiles.description LIKE ? THEN 4 ELSE 0 END +
                    CASE WHEN profiles.website LIKE ? THEN 3 ELSE 0 END +
                    CASE WHEN profiles.twitter LIKE ? THEN 3 ELSE 0 END +
                    CASE WHEN profiles.github LIKE ? THEN 3 ELSE 0 END +
                    CASE WHEN profiles.linkedin LIKE ? THEN 3 ELSE 0 END +
                    CASE WHEN profiles.instagram LIKE ? THEN 2 ELSE 0 END +
                    CASE WHEN profiles.facebook LIKE ? THEN 2 ELSE 0 END +
                    CASE WHEN profiles.youtube LIKE ? THEN 2 ELSE 0 END +
                    CASE WHEN profiles.discord LIKE ? THEN 2 ELSE 0 END
                ) as relevance_score', 
                array_fill(0, 14, '%' . $this->query . '%'))
                ->leftJoin('profiles', 'users.id', '=', 'profiles.user_id')
                ->orderBy('relevance_score', $this->sortDirection)
                ->orderBy('users.name', 'asc');
        } else {
            // Standard sorting
            switch ($this->sortBy) {
                case 'name':
                    $query->orderBy('name', $this->sortDirection);
                    break;
                case 'location':
                    $query->whereHas('profile')->orderBy('profiles.location', $this->sortDirection);
                    break;
                case 'join_date':
                    $query->orderBy('created_at', $this->sortDirection);
                    break;
                default:
                    $query->orderBy('name', 'asc');
            }
        }

        return $query->paginate(12);
    }

    public function getGenderOptionsProperty()
    {
        return [
            '' => 'All Genders',
            'male' => 'Male',
            'female' => 'Female',
            'other' => 'Other',
            'prefer not to say' => 'Prefer not to say'
        ];
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
        <h1 class="text-3xl font-bold text-gray-900 dark:text-white">Advanced Profile Search</h1>
        <p class="text-gray-600 dark:text-gray-300 mt-2">Find users by name, bio, location, or any profile information</p>
    </div>

    <!-- Search Form -->
    <div class="bg-white dark:bg-gray-800 rounded-lg shadow-sm border border-gray-200 dark:border-gray-700 p-6 mb-6">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div class="md:col-span-2">
                <flux:input wire:model.live="query" :label="__('Search profiles')" placeholder="Search by name, bio, location, social links..." icon="magnifying-glass" />
                <p class="text-xs text-gray-500 mt-1">Tip: Use multiple words for better results (e.g., "developer Nairobi")</p>
            </div>
            <div>
                <flux:input wire:model.live="location" :label="__('Location')" placeholder="City, Country" icon="map-pin" />
            </div>
            <div>
                <flux:select wire:model.live="gender" :label="__('Gender')">
                    @foreach ($genderOptions as $value => $label)
                        <option value="{{ $value }}">{{ $label }}</option>
                    @endforeach
                </flux:select>
            </div>
        </div>

        <!-- Sort Options -->
        <div class="flex items-center justify-between mt-6">
            <div class="flex items-center space-x-4">
                <span class="text-sm text-gray-600 dark:text-gray-300">Sort by:</span>
                <div class="flex items-center space-x-2">
                    <button wire:click="sortBy('relevance')" class="flex items-center space-x-1 text-sm {{ $sortBy === 'relevance' ? 'text-blue-600 font-semibold' : 'text-gray-600 dark:text-gray-300' }}">
                        <span>Relevance</span>
                        @if ($sortBy === 'relevance')
                            <span>{{ $sortDirection === 'desc' ? '▼' : '▲' }}</span>
                        @endif
                    </button>
                    <button wire:click="sortBy('name')" class="flex items-center space-x-1 text-sm {{ $sortBy === 'name' ? 'text-blue-600 font-semibold' : 'text-gray-600 dark:text-gray-300' }}">
                        <span>Name</span>
                        @if ($sortBy === 'name')
                            <span>{{ $sortDirection === 'asc' ? '▲' : '▼' }}</span>
                        @endif
                    </button>
                    <button wire:click="sortBy('location')" class="flex items-center space-x-1 text-sm {{ $sortBy === 'location' ? 'text-blue-600 font-semibold' : 'text-gray-600 dark:text-gray-300' }}">
                        <span>Location</span>
                        @if ($sortBy === 'location')
                            <span>{{ $sortDirection === 'asc' ? '▲' : '▼' }}</span>
                        @endif
                    </button>
                    <button wire:click="sortBy('join_date')" class="flex items-center space-x-1 text-sm {{ $sortBy === 'join_date' ? 'text-blue-600 font-semibold' : 'text-gray-600 dark:text-gray-300' }}">
                        <span>Join Date</span>
                        @if ($sortBy === 'join_date')
                            <span>{{ $sortDirection === 'asc' ? '▲' : '▼' }}</span>
                        @endif
                    </button>
                </div>
            </div>
            <div class="text-sm text-gray-500 dark:text-gray-400">
                Showing {{ $users->firstItem() ?? 0 }}-{{ $users->lastItem() ?? 0 }} of {{ $users->total() }} results
            </div>
        </div>
    </div>

    <!-- Search Results -->
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
                        <p class="text-sm text-gray-500 dark:text-gray-400">{{ $user->email }}</p>
                        
                        @if ($user->profile && $user->profile->location)
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

                        <!-- Gender -->
                        @if ($user->profile && $user->profile->gender)
                            <div class="mt-3 inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200">
                                {{ ucfirst($user->profile->gender) }}
                            </div>
                        @endif

                        <!-- Social Links Preview -->
                        @if ($user->profile && ($user->profile->twitter || $user->profile->github || $user->profile->linkedin))
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

                        <!-- Relevance Score (if searching) -->
                        @if ($query && isset($user->relevance_score) && $user->relevance_score > 0)
                            <div class="mt-4 text-xs text-gray-500 dark:text-gray-400">
                                Relevance: {{ $user->relevance_score }}
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
            <p class="text-gray-500 mt-2">Try adjusting your search criteria or filters</p>
            <div class="mt-4">
                <button wire:click="$set('query', '')" wire:click="$set('location', '')" wire:click="$set('gender', '')" class="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700">
                    Clear Filters
                </button>
            </div>
        </div>
    @endif
</div>