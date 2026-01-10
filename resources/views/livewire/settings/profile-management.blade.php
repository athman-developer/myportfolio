<?php

use App\Models\Profile;
use Illuminate\Support\Facades\Auth;
use Livewire\Attributes\Validate;
use Livewire\Volt\Component;
use Livewire\WithFileUploads;

new class extends Component {
    use WithFileUploads;

    public Profile $profile;
    public array $social = [];
    #[Validate('image|max:1024')]
    public $avatar;

    /**
     * Mount the component.
     */
    public function mount(): void
    {
        $this->profile = Auth::user()->profile ?? Auth::user()->profile()->create();
        
        // Initialize social media fields
        $this->social = [
            'website' => $this->profile->website ?? '',
            'twitter' => $this->profile->twitter ?? '',
            'github' => $this->profile->github ?? '',
            'linkedin' => $this->profile->linkedin ?? '',
            'instagram' => $this->profile->instagram ?? '',
            'facebook' => $this->profile->facebook ?? '',
            'youtube' => $this->profile->youtube ?? '',
            'discord' => $this->profile->discord ?? '',
        ];
    }

    /**
     * Update the profile information.
     */
    public function updateProfile(): void
    {
        $this->validate([
            'profile.bio' => 'nullable|string|max:500',
            'profile.location' => 'nullable|string|max:255',
            'profile.phone' => 'nullable|string|max:20',
            'profile.birthday' => 'nullable|date',
            'profile.gender' => 'nullable|in:male,female,other,prefer not to say',
            'profile.description' => 'nullable|string',
            'profile.profile_visibility' => 'required|in:public,private',
            'profile.show_email' => 'boolean',
            'profile.show_phone' => 'boolean',
            'profile.show_location' => 'boolean',
            'profile.show_social_links' => 'boolean',
            'profile.allow_messages' => 'boolean',
            'social.website' => 'nullable|url|max:255',
            'social.twitter' => 'nullable|string|max:50',
            'social.github' => 'nullable|string|max:50',
            'social.linkedin' => 'nullable|string|max:255',
            'social.instagram' => 'nullable|string|max:50',
            'social.facebook' => 'nullable|string|max:50',
            'social.youtube' => 'nullable|string|max:255',
            'social.discord' => 'nullable|string|max:50',
            'avatar' => 'nullable|image|max:1024',
        ]);

        $this->profile->fill([
            'bio' => $this->profile->bio,
            'location' => $this->profile->location,
            'phone' => $this->profile->phone,
            'birthday' => $this->profile->birthday,
            'gender' => $this->profile->gender,
            'description' => $this->profile->description,
            'profile_visibility' => $this->profile->profile_visibility,
            'show_email' => $this->profile->show_email,
            'show_phone' => $this->profile->show_phone,
            'show_location' => $this->profile->show_location,
            'show_social_links' => $this->profile->show_social_links,
            'allow_messages' => $this->profile->allow_messages,
        ]);

        // Update social media fields
        foreach ($this->social as $field => $value) {
            $this->profile->$field = $value;
        }

        // Handle avatar upload
        if ($this->avatar) {
            $path = $this->avatar->store('avatars', 'public');
            
            $this->profile->avatar = $path;
            $this->profile->avatar_original_name = $this->avatar->getClientOriginalName();
            $this->profile->avatar_mime_type = $this->avatar->getMimeType();
            $this->profile->avatar_size = $this->avatar->getSize();
        }

        $this->profile->save();

        $this->dispatch('profile-updated');
    }

    /**
     * Remove the avatar.
     */
    public function removeAvatar(): void
    {
        if ($this->profile->avatar) {
            \Storage::disk('public')->delete($this->profile->avatar);
            $this->profile->avatar = null;
            $this->profile->avatar_original_name = null;
            $this->profile->avatar_mime_type = null;
            $this->profile->avatar_size = null;
            $this->profile->save();
            $this->dispatch('profile-updated');
        }
    }
}; ?>

<section class="w-full">
    @include('partials.settings-heading')

    <x-settings.layout :heading="__('Profile Management')" :subheading="__('Manage your profile information and social links')">
        <form wire:submit="updateProfile" class="my-6 w-full space-y-6">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                <!-- Avatar Section -->
                <div class="md:col-span-1">
                    <div class="bg-gray-50 dark:bg-gray-700 rounded-lg p-4">
                        <h3 class="font-semibold text-gray-900 dark:text-white mb-3">Profile Picture</h3>
                        <div class="flex items-center space-x-4">
                            <div class="w-20 h-20 bg-gray-200 dark:bg-gray-600 rounded-full flex items-center justify-center text-xl font-bold text-gray-600 dark:text-gray-300">
                                {{ $profile->avatar ? 'Image' : (Auth::user()->initials()) }}
                            </div>
                            <div class="flex-1">
                                <input type="file" wire:model="avatar" class="block w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700 hover:file:bg-blue-100" />
                                <p class="text-xs text-gray-500 mt-1">PNG, JPG, GIF up to 1MB</p>
                                @error('avatar') <span class="text-red-500 text-sm">{{ $message }}</span> @enderror
                            </div>
                        </div>
                        @if ($avatar)
                            <div class="mt-3">
                                <img src="{{ $avatar->temporaryUrl() }}" alt="Preview" class="w-32 h-32 object-cover rounded-lg" />
                            </div>
                        @elseif ($profile->avatar)
                            <div class="mt-3">
                                <img src="{{ Storage::url($profile->avatar) }}" alt="Current Avatar" class="w-32 h-32 object-cover rounded-lg" />
                                <button wire:click="removeAvatar" class="mt-2 text-red-600 hover:text-red-800 text-sm">Remove Avatar</button>
                            </div>
                        @endif
                    </div>
                </div>

                <div class="md:col-span-2">
                    <flux:textarea wire:model="profile.bio" :label="__('Bio')" placeholder="{{ __('Tell us about yourself...') }}" rows="4" />
                </div>

                <div>
                    <flux:input wire:model="profile.location" :label="__('Location')" type="text" placeholder="{{ __('City, Country') }}" />
                </div>

                <div>
                    <flux:input wire:model="profile.phone" :label="__('Phone Number')" type="tel" placeholder="{{ __('+1234567890') }}" />
                </div>

                <div>
                    <flux:input wire:model="profile.birthday" :label="__('Birthday')" type="date" />
                </div>

                <div>
                    <flux:select wire:model="profile.gender" :label="__('Gender')">
                        <option value="">{{ __('Select gender') }}</option>
                        <option value="male">{{ __('Male') }}</option>
                        <option value="female">{{ __('Female') }}</option>
                        <option value="other">{{ __('Other') }}</option>
                        <option value="prefer not to say">{{ __('Prefer not to say') }}</option>
                    </flux:select>
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="md:col-span-2">
                    <flux:textarea wire:model="profile.description" :label="__('Description')" placeholder="{{ __('Detailed description about yourself...') }}" rows="6" />
                </div>
            </div>

            <!-- Privacy Settings -->
            <div class="border-t border-gray-200 dark:border-gray-700 pt-6">
                <h3 class="text-lg font-semibold mb-4">{{ __('Privacy Settings') }}</h3>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <flux:select wire:model="profile.profile_visibility" :label="__('Profile Visibility')">
                            <option value="public">{{ __('Public (visible to everyone)') }}</option>
                            <option value="private">{{ __('Private (visible only to you)') }}</option>
                        </flux:select>
                    </div>

                    <div class="space-y-4">
                        <div class="flex items-center space-x-3">
                            <flux:toggle wire:model="profile.show_email" />
                            <div>
                                <div class="font-medium text-gray-900 dark:text-white">{{ __('Show email address') }}</div>
                                <div class="text-sm text-gray-500 dark:text-gray-400">Allow others to see your email</div>
                            </div>
                        </div>

                        <div class="flex items-center space-x-3">
                            <flux:toggle wire:model="profile.show_phone" />
                            <div>
                                <div class="font-medium text-gray-900 dark:text-white">{{ __('Show phone number') }}</div>
                                <div class="text-sm text-gray-500 dark:text-gray-400">Allow others to see your phone</div>
                            </div>
                        </div>

                        <div class="flex items-center space-x-3">
                            <flux:toggle wire:model="profile.show_location" />
                            <div>
                                <div class="font-medium text-gray-900 dark:text-white">{{ __('Show location') }}</div>
                                <div class="text-sm text-gray-500 dark:text-gray-400">Display your location on your profile</div>
                            </div>
                        </div>

                        <div class="flex items-center space-x-3">
                            <flux:toggle wire:model="profile.show_social_links" />
                            <div>
                                <div class="font-medium text-gray-900 dark:text-white">{{ __('Show social links') }}</div>
                                <div class="text-sm text-gray-500 dark:text-gray-400">Display your social media links</div>
                            </div>
                        </div>

                        <div class="flex items-center space-x-3">
                            <flux:toggle wire:model="profile.allow_messages" />
                            <div>
                                <div class="font-medium text-gray-900 dark:text-white">{{ __('Allow messages') }}</div>
                                <div class="text-sm text-gray-500 dark:text-gray-400">Allow others to send you messages</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="border-t border-gray-200 dark:border-gray-700 pt-6">
                <h3 class="text-lg font-semibold mb-4">{{ __('Social Links') }}</h3>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                    <div>
                        <flux:input wire:model="social.website" :label="__('Website')" type="url" placeholder="https://example.com" />
                    </div>

                    <div>
                        <flux:input wire:model="social.twitter" :label="__('Twitter/X')" type="text" placeholder="@username" />
                    </div>

                    <div>
                        <flux:input wire:model="social.github" :label="__('GitHub')" type="text" placeholder="username" />
                    </div>

                    <div>
                        <flux:input wire:model="social.linkedin" :label="__('LinkedIn')" type="text" placeholder="profile-url" />
                    </div>

                    <div>
                        <flux:input wire:model="social.instagram" :label="__('Instagram')" type="text" placeholder="@username" />
                    </div>

                    <div>
                        <flux:input wire:model="social.facebook" :label="__('Facebook')" type="text" placeholder="username" />
                    </div>

                    <div>
                        <flux:input wire:model="social.youtube" :label="__('YouTube')" type="text" placeholder="channel-url" />
                    </div>

                    <div>
                        <flux:input wire:model="social.discord" :label="__('Discord')" type="text" placeholder="username#1234" />
                    </div>
                </div>
            </div>

            <div class="flex items-center gap-4">
                <div class="flex items-center justify-end">
                    <flux:button variant="primary" type="submit" class="w-full" data-test="update-profile-button">
                        {{ __('Save Profile') }}
                    </flux:button>
                </div>

                <x-action-message class="me-3" on="profile-updated">
                    {{ __('Profile saved successfully.') }}
                </x-action-message>
            </div>
        </form>
    </x-settings.layout>
</section>